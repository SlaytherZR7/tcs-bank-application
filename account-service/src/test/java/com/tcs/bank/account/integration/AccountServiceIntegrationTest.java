package com.tcs.bank.account.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcs.bank.account.exceptions.InsufficientFundsException;
import com.tcs.bank.account.messaging.AccountMessagingConfiguration;
import com.tcs.bank.account.model.dto.CreateMovementRequest;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import com.tcs.bank.account.repository.ClientProjectionRepository;
import com.tcs.bank.account.service.MovementService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AccountServiceIntegrationTest {

    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ClientProjectionRepository clientProjectionRepository;

    @Autowired
    private MovementService movementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Test
    @DisplayName("Flyway applies every migration to PostgreSQL when the service starts")
    void startup_shouldApplyAllFlywayMigrations_whenServiceStarts() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("a ClientCreated event enables opening accounts, registering movements and reporting them")
    void clientCreatedEvent_shouldEnableAccountsMovementsAndReport_whenClientIsActive() throws Exception {
        givenActiveClient(101L, "Marianela Montalvo");

        mockMvc.perform(post("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("225487", "Corriente", "100", 101L)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.saldoDisponible").value(100));
        givenAccount("496825", "540", 101L);

        mockMvc.perform(post("/api/movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementJson("225487", "600")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(700.00));
        mockMvc.perform(post("/api/movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementJson("496825", "-540")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saldo").value(0.00));

        assertThat(currentBalance("225487")).isEqualByComparingTo("700.00");
        assertThat(currentBalance("496825")).isEqualByComparingTo("0.00");

        LocalDate today = LocalDate.now(clock);
        mockMvc.perform(get("/api/reportes")
                .param("clienteId", "101")
                .param("fechaInicio", today.toString())
                .param("fechaFin", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].Cliente").value("Marianela Montalvo"))
                .andExpect(jsonPath("$[0]['Numero Cuenta']").value("225487"))
                .andExpect(jsonPath("$[0].Movimiento").value(600.00))
                .andExpect(jsonPath("$[0]['Saldo Disponible']").value(700.00))
                .andExpect(jsonPath("$[1]['Numero Cuenta']").value("496825"))
                .andExpect(jsonPath("$[1].Movimiento").value(-540.00))
                .andExpect(jsonPath("$[1]['Saldo Disponible']").value(0.00));
    }

    @Test
    @DisplayName("a withdrawal without funds returns 'Saldo no disponible' and persists nothing")
    void createMovement_shouldRejectAndPersistNothing_whenFundsAreInsufficient() throws Exception {
        givenActiveClient(102L, "Jose Lema");
        givenAccount("478758", "100", 102L);

        mockMvc.perform(post("/api/movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movementJson("478758", "-150")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Saldo no disponible"));

        assertThat(currentBalance("478758")).isEqualByComparingTo("100.00");
        assertThat(movementCount("478758")).isZero();
    }

    @Test
    @DisplayName("concurrent withdrawals never spend the same balance twice")
    void createMovement_shouldNeverOverdraw_whenWithdrawalsRunConcurrently() throws Exception {
        givenActiveClient(103L, "Juan Osorio");
        givenAccount("495878", "100", 103L);

        int attempts = 10;
        CountDownLatch startGate = new CountDownLatch(1);
        Callable<Boolean> withdrawal = () -> {
            startGate.await();
            try {
                movementService.create(new CreateMovementRequest("495878", new BigDecimal("-20.00")));
                return true;
            } catch (InsufficientFundsException exception) {
                return false;
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < attempts; i++) {
                results.add(executor.submit(withdrawal));
            }
            startGate.countDown();

            long succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    succeeded++;
                }
            }

            // 100 / 20 = 5 withdrawals fit; the pessimistic lock serializes the rest into
            // rejections
            assertThat(succeeded).isEqualTo(5);
            assertThat(currentBalance("495878")).isEqualByComparingTo("0.00");
            assertThat(movementCount("495878")).isEqualTo(5);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("a ClientDeactivated event blocks opening new accounts")
    void createAccount_shouldReturnConflict_whenClientWasDeactivated() throws Exception {
        givenActiveClient(104L, "Ana Pérez");
        publishClientEvent(104L, "Ana Pérez", "ClientDeactivated", "DELETED", "2026-01-02T10:00:00Z");
        awaitClientStatus(104L, ClientStatus.DELETED);

        mockMvc.perform(post("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("585545", "Corriente", "1000", 104L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("El cliente con ID 104 no está activo"));
    }

    @Test
    @DisplayName("opening an account with a number already in use returns 409")
    void createAccount_shouldReturnConflict_whenAccountNumberExists() throws Exception {
        givenActiveClient(105L, "Luis Vega");
        givenAccount("700001", "50", 105L);

        mockMvc.perform(post("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("700001", "Ahorros", "10", 105L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una cuenta con ese número"));
    }

    @Test
    @DisplayName("opening an account for a client whose event never arrived returns 404")
    void createAccount_shouldReturnNotFound_whenClientIsUnknown() throws Exception {
        mockMvc.perform(post("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("700002", "Ahorros", "10", 999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontró el cliente con ID 999"));
    }

    @Test
    @DisplayName("opening an account with invalid data returns 400 and persists nothing")
    void createAccount_shouldReturnBadRequest_whenInitialBalanceIsNegative() throws Exception {
        givenActiveClient(106L, "Rosa Mora");

        mockMvc.perform(post("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson("700003", "Ahorros", "-1", 106L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        Integer accounts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE account_number = '700003'", Integer.class);
        assertThat(accounts).isZero();
    }

    @Test
    @DisplayName("an unreadable client event is routed to the dead letter queue")
    void clientEventListener_shouldDeadLetterMessage_whenPayloadIsInvalid() {
        rabbitTemplate.convertAndSend(
                AccountMessagingConfiguration.CLIENT_EVENTS_EXCHANGE,
                AccountMessagingConfiguration.CLIENT_EVENTS_ROUTING_KEY,
                "not-a-json-event");

        Message deadLetter = rabbitTemplate.receive(
                AccountMessagingConfiguration.DEAD_LETTER_QUEUE, EVENT_TIMEOUT.toMillis());

        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody())).isEqualTo("not-a-json-event");
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private void givenActiveClient(Long clientId, String name) {
        publishClientEvent(clientId, name, "ClientCreated", "ACTIVE", "2026-01-01T10:00:00Z");
        awaitClientStatus(clientId, ClientStatus.ACTIVE);
    }

    private void publishClientEvent(
            Long clientId, String name, String eventType, String status, String occurredAt) {
        String payload = """
                {"eventId":"%s","schemaVersion":1,"eventType":"%s","clientId":%d,
                 "clientName":"%s","clientStatus":"%s","occurredAt":"%s"}
                """.formatted(UUID.randomUUID(), eventType, clientId, name, status, occurredAt);
        rabbitTemplate.convertAndSend(
                AccountMessagingConfiguration.CLIENT_EVENTS_EXCHANGE,
                AccountMessagingConfiguration.CLIENT_EVENTS_ROUTING_KEY,
                payload);
    }

    private void awaitClientStatus(Long clientId, ClientStatus expectedStatus) {
        await().atMost(EVENT_TIMEOUT).until(() -> clientProjectionRepository.findById(clientId)
                .map(projection -> projection.getStatus() == expectedStatus)
                .orElse(false));
    }

    private void givenAccount(String accountNumber, String initialBalance, Long clientId) throws Exception {
        mockMvc.perform(post("/api/cuentas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(accountJson(accountNumber, "Ahorros", initialBalance, clientId)))
                .andExpect(status().isCreated());
    }

    private BigDecimal currentBalance(String accountNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT current_balance FROM accounts WHERE account_number = ?", BigDecimal.class, accountNumber);
    }

    private long movementCount(String accountNumber) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM movements m JOIN accounts a ON a.account_id = m.account_id
                WHERE a.account_number = ?
                """, Long.class, accountNumber);
    }

    private static String accountJson(String number, String type, String initialBalance, Long clientId) {
        return """
                {"numeroCuenta":"%s","tipoCuenta":"%s","saldoInicial":%s,"estado":true,"clienteId":%d}
                """.formatted(number, type, initialBalance, clientId);
    }

    private static String movementJson(String accountNumber, String amount) {
        return """
                {"numeroCuenta":"%s","valor":%s}
                """.formatted(accountNumber, amount);
    }
}
