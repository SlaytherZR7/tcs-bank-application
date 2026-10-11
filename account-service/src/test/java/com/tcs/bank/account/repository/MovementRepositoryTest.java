package com.tcs.bank.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.entity.Movement;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import com.tcs.bank.account.model.enumeration.AccountType;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import com.tcs.bank.account.model.enumeration.MovementType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class MovementRepositoryTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2022, 2, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2022, 2, 28, 23, 59, 59);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MovementRepository movementRepository;

    private Account checking;
    private Account savings;
    private Account otherClientAccount;

    @BeforeEach
    void setUp() {
        ClientProjection marianela = client(2L, "Marianela Montalvo");
        ClientProjection jose = client(1L, "Jose Lema");
        checking = account("225487", AccountType.CHECKING, "100.00", marianela);
        savings = account("496825", AccountType.SAVINGS, "540.00", marianela);
        otherClientAccount = account("478758", AccountType.SAVINGS, "2000.00", jose);
    }

    @Test
    @DisplayName("findClientMovementsBetween returns only the client's movements in the inclusive range, grouped by account")
    void findClientMovementsBetween_shouldReturnClientMovementsInRange_orderedByAccountAndDate() {
        Movement savingsWithdrawal = movement(savings, "-540.00", "0.00", LocalDateTime.of(2022, 2, 8, 15, 30));
        Movement checkingDeposit = movement(checking, "600.00", "700.00", LocalDateTime.of(2022, 2, 10, 10, 0));
        Movement checkingOnLastDay = movement(checking, "-50.00", "650.00", TO);
        movement(checking, "-100.00", "550.00", LocalDateTime.of(2022, 3, 1, 0, 0));
        movement(otherClientAccount, "-575.00", "1425.00", LocalDateTime.of(2022, 2, 9, 9, 0));
        entityManager.flush();

        List<Movement> movements = movementRepository.findClientMovementsBetween(2L, FROM, TO);

        assertThat(movements).containsExactly(checkingDeposit, checkingOnLastDay, savingsWithdrawal);
    }

    @Test
    @DisplayName("findLastMovementsUpTo returns the latest movement per account up to the cut-off, breaking date ties by ID")
    void findLastMovementsUpTo_shouldReturnLatestMovementPerAccount_ignoringLaterOnes() {
        LocalDateTime sameInstant = LocalDateTime.of(2022, 1, 20, 12, 0);
        movement(checking, "200.00", "300.00", LocalDateTime.of(2022, 1, 5, 9, 0));
        movement(checking, "-200.00", "100.00", sameInstant);
        Movement checkingLast = movement(checking, "400.00", "500.00", sameInstant);
        movement(checking, "-500.00", "0.00", LocalDateTime.of(2022, 3, 2, 8, 0));
        Movement otherLast = movement(otherClientAccount, "-575.00", "1425.00", LocalDateTime.of(2022, 2, 9, 9, 0));
        entityManager.flush();

        List<Movement> lastMovements = movementRepository.findLastMovementsUpTo(
                List.of(checking.getId(), savings.getId(), otherClientAccount.getId()), TO);

        assertThat(lastMovements).containsExactlyInAnyOrder(checkingLast, otherLast);
    }

    private ClientProjection client(Long clientId, String name) {
        ClientProjection client = new ClientProjection();
        client.setClientId(clientId);
        client.setName(name);
        client.setStatus(ClientStatus.ACTIVE);
        client.setLastEventId("evt-" + clientId);
        client.setLastEventAt(Instant.parse("2022-01-01T00:00:00Z"));
        return entityManager.persist(client);
    }

    private Account account(String number, AccountType type, String initialBalance, ClientProjection client) {
        Account account = new Account();
        account.setAccountNumber(number);
        account.setAccountType(type);
        account.setInitialBalance(new BigDecimal(initialBalance));
        account.setCurrentBalance(new BigDecimal(initialBalance));
        account.setStatus(AccountStatus.ACTIVE);
        account.setClient(client);
        return entityManager.persist(account);
    }

    private Movement movement(Account account, String amount, String resultingBalance, LocalDateTime date) {
        Movement movement = new Movement();
        movement.setAccount(account);
        movement.setAmount(new BigDecimal(amount));
        movement.setMovementType(MovementType.fromAmount(new BigDecimal(amount)));
        movement.setResultingBalance(new BigDecimal(resultingBalance));
        movement.setMovementDate(date);
        return entityManager.persist(movement);
    }
}
