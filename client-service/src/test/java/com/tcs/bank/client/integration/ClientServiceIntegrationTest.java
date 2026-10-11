package com.tcs.bank.client.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.tcs.bank.client.messaging.ClientMessagingConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs client-service with its real configuration against PostgreSQL and
 * RabbitMQ containers.
 *
 * <p>
 * Scope: verifies the HTTP → service → repository → PostgreSQL flow and that
 * lifecycle events reach
 * the shared exchange with the JSON contract account-service consumes. A
 * temporary queue bound to the
 * exchange plays the role of account-service, whose consuming side has its own
 * integration test.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClientServiceIntegrationTest {

        private static final long EVENT_TIMEOUT_MILLIS = Duration.ofSeconds(10).toMillis();
        private static final long NO_EVENT_TIMEOUT_MILLIS = Duration.ofSeconds(1).toMillis();

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private RabbitTemplate rabbitTemplate;

        @Autowired
        private AmqpAdmin amqpAdmin;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private Queue eventsQueue;

        @BeforeEach
        void bindEventsQueue() {
                // Not auto-delete: each receive() opens and cancels a consumer, which would
                // drop an auto-delete queue
                eventsQueue = new Queue("client-events-it-" + UUID.randomUUID(), false, false, false);
                amqpAdmin.declareQueue(eventsQueue);
                Binding binding = BindingBuilder.bind(eventsQueue)
                                .to(new TopicExchange(ClientMessagingConfiguration.EXCHANGE_NAME))
                                .with(ClientMessagingConfiguration.ROUTING_KEY);
                amqpAdmin.declareBinding(binding);
        }

        @AfterEach
        void deleteEventsQueue() {
                amqpAdmin.deleteQueue(eventsQueue.getName());
        }

        @Test
        @DisplayName("Flyway applies the client migrations to PostgreSQL when the service starts")
        void startup_shouldApplyFlywayMigrations_whenServiceStarts() {
                List<String> versions = jdbcTemplate.queryForList(
                                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                                String.class);

                assertThat(versions).containsExactly("1");
        }

        @Test
        @DisplayName("creating a client persists it with a BCrypt hash and publishes ClientCreated without the password")
        void create_shouldPersistClientAndPublishEvent_whenPayloadIsValid() throws Exception {
                String response = mockMvc.perform(post("/api/clientes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(clientJson("Jose Lema", "1712345678", "1234")))
                                .andExpect(status().isCreated())
                                .andExpect(header().exists("Location"))
                                .andExpect(jsonPath("$.nombre").value("Jose Lema"))
                                .andExpect(jsonPath("$.contrasena").doesNotExist())
                                .andReturn().getResponse().getContentAsString();
                long clientId = ((Number) JsonPath.read(response, "$.clienteId")).longValue();

                String storedHash = jdbcTemplate.queryForObject(
                                "SELECT password_hash FROM clients WHERE client_id = ?", String.class, clientId);
                assertThat(storedHash).isNotEqualTo("1234").startsWith("$2");
                assertThat(passwordEncoder.matches("1234", storedHash)).isTrue();

                String event = receiveEvent();
                assertThat((String) JsonPath.read(event, "$.eventType")).isEqualTo("ClientCreated");
                assertThat(((Number) JsonPath.read(event, "$.clientId")).longValue()).isEqualTo(clientId);
                assertThat((String) JsonPath.read(event, "$.clientName")).isEqualTo("Jose Lema");
                assertThat((String) JsonPath.read(event, "$.clientStatus")).isEqualTo("ACTIVE");
                assertThat((Integer) JsonPath.read(event, "$.schemaVersion")).isEqualTo(1);
                assertThat(event).doesNotContain("1234").doesNotContain(storedHash);
        }

        @Test
        @DisplayName("deleting a client keeps the row as DELETED and publishes ClientDeactivated")
        void delete_shouldMarkClientDeletedAndPublishEvent_whenClientExists() throws Exception {
                long clientId = createClient("Marianela Montalvo", "1798765432");
                receiveEvent();

                mockMvc.perform(delete("/api/clientes/{id}", clientId))
                                .andExpect(status().isNoContent());

                String status = jdbcTemplate.queryForObject(
                                "SELECT status FROM clients WHERE client_id = ?", String.class, clientId);
                assertThat(status).isEqualTo("DELETED");

                String event = receiveEvent();
                assertThat((String) JsonPath.read(event, "$.eventType")).isEqualTo("ClientDeactivated");
                assertThat((String) JsonPath.read(event, "$.clientStatus")).isEqualTo("DELETED");
        }

        @Test
        @DisplayName("a duplicated identification returns 409, keeps a single row and publishes no event")
        void create_shouldReturnConflictWithoutEvent_whenIdentificationExists() throws Exception {
                createClient("Juan Osorio", "1755555555");
                receiveEvent();

                mockMvc.perform(post("/api/clientes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(clientJson("Otro Cliente", "1755555555", "9999")))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Ya existe un cliente con esa identificación"));

                Integer rows = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM clients WHERE identification = '1755555555'", Integer.class);
                assertThat(rows).isEqualTo(1);
                assertThat(rabbitTemplate.receive(eventsQueue.getName(), NO_EVENT_TIMEOUT_MILLIS)).isNull();
        }

        @Test
        @DisplayName("requesting a client that does not exist returns 404")
        void findById_shouldReturnNotFound_whenClientDoesNotExist() throws Exception {
                mockMvc.perform(get("/api/clientes/{id}", 999_999))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("No se encontró el cliente con ID 999999"));
        }

        @Test
        @DisplayName("an invalid client returns 400 and persists nothing")
        void create_shouldReturnBadRequestAndPersistNothing_whenAgeIsNotPositive() throws Exception {
                String invalidClient = clientJson("Edad Invalida", "1766666666", "1234").replace("\"edad\":30",
                                "\"edad\":0");

                mockMvc.perform(post("/api/clientes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidClient))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));

                Integer rows = jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM clients WHERE identification = '1766666666'", Integer.class);
                assertThat(rows).isZero();
        }

        // ─── Helpers ────────────────────────────────────────────────────────────────

        private long createClient(String name, String identification) throws Exception {
                String response = mockMvc.perform(post("/api/clientes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(clientJson(name, identification, "1234")))
                                .andExpect(status().isCreated())
                                .andReturn().getResponse().getContentAsString();
                return ((Number) JsonPath.read(response, "$.clienteId")).longValue();
        }

        private String receiveEvent() {
                Message message = rabbitTemplate.receive(eventsQueue.getName(), EVENT_TIMEOUT_MILLIS);
                assertThat(message).as("client lifecycle event").isNotNull();
                return new String(message.getBody());
        }

        private static String clientJson(String name, String identification, String password) {
                return """
                                {"nombre":"%s","genero":"Masculino","edad":30,"identificacion":"%s",
                                 "direccion":"Otavalo sn y principal","telefono":"098254785","contrasena":"%s","estado":true}
                                """
                                .formatted(name, identification, password);
        }
}
