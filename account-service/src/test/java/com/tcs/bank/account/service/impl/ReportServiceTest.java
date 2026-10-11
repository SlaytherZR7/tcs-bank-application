package com.tcs.bank.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.bank.account.exceptions.ClientProjectionNotFoundException;
import com.tcs.bank.account.model.dto.AccountStatementReportItem;
import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.entity.Movement;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import com.tcs.bank.account.model.enumeration.AccountType;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import com.tcs.bank.account.model.enumeration.MovementType;
import com.tcs.bank.account.repository.AccountRepository;
import com.tcs.bank.account.repository.ClientProjectionRepository;
import com.tcs.bank.account.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ClientProjectionRepository clientProjectionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                clientProjectionRepository, accountRepository, movementRepository);
    }

    @Test
    @DisplayName("generateAccountStatement produces report matching technical test example for Marianela Montalvo")
    void generateAccountStatement_shouldProduceReport_forMarianelaCase() {
        Long clientId = 2L;
        LocalDate startDate = LocalDate.of(2022, 2, 1);
        LocalDate endDate = LocalDate.of(2022, 2, 28);

        ClientProjection client = new ClientProjection();
        client.setClientId(clientId);
        client.setName("Marianela Montalvo");
        client.setStatus(ClientStatus.ACTIVE);
        client.setLastEventId("evt-1");
        client.setLastEventAt(Instant.now());

        Account checkingAccount = new Account();
        checkingAccount.setId(10L);
        checkingAccount.setAccountNumber("225487");
        checkingAccount.setAccountType(AccountType.CHECKING);
        checkingAccount.setInitialBalance(new BigDecimal("100.00"));
        checkingAccount.setCurrentBalance(new BigDecimal("700.00"));
        checkingAccount.setStatus(AccountStatus.ACTIVE);
        checkingAccount.setClient(client);

        Account savingsAccount = new Account();
        savingsAccount.setId(20L);
        savingsAccount.setAccountNumber("496825");
        savingsAccount.setAccountType(AccountType.SAVINGS);
        savingsAccount.setInitialBalance(new BigDecimal("540.00"));
        savingsAccount.setCurrentBalance(BigDecimal.ZERO);
        savingsAccount.setStatus(AccountStatus.ACTIVE);
        savingsAccount.setClient(client);

        Movement depositMovement = new Movement();
        depositMovement.setId(100L);
        depositMovement.setAccount(checkingAccount);
        depositMovement.setMovementDate(LocalDateTime.of(2022, 2, 10, 10, 0));
        depositMovement.setAmount(new BigDecimal("600.00"));
        depositMovement.setResultingBalance(new BigDecimal("700.00"));
        depositMovement.setMovementType(MovementType.INCOME);

        Movement withdrawalMovement = new Movement();
        withdrawalMovement.setId(200L);
        withdrawalMovement.setAccount(savingsAccount);
        withdrawalMovement.setMovementDate(LocalDateTime.of(2022, 2, 8, 15, 30));
        withdrawalMovement.setAmount(new BigDecimal("-540.00"));
        withdrawalMovement.setResultingBalance(BigDecimal.ZERO);
        withdrawalMovement.setMovementType(MovementType.EXPENSE);

        when(clientProjectionRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(accountRepository.findByClient_ClientIdOrderById(clientId))
                .thenReturn(List.of(checkingAccount, savingsAccount));
        when(movementRepository.findClientMovementsBetween(
                clientId, startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX)))
                .thenReturn(List.of(depositMovement, withdrawalMovement));

        List<AccountStatementReportItem> report = reportService.generateAccountStatement(
                clientId, startDate, endDate);

        verify(movementRepository, never()).findLastMovementsUpTo(any(), any());
        assertEquals(2, report.size());

        AccountStatementReportItem item1 = report.get(0);
        assertEquals("10/2/2022", item1.date());
        assertEquals("Marianela Montalvo", item1.clientName());
        assertEquals("225487", item1.accountNumber());
        assertEquals(AccountType.CHECKING, item1.accountType());
        assertEquals(new BigDecimal("100.00"), item1.initialBalance());
        assertTrue(item1.status());
        assertEquals(new BigDecimal("600.00"), item1.amount());
        assertEquals(new BigDecimal("700.00"), item1.resultingBalance());

        AccountStatementReportItem item2 = report.get(1);
        assertEquals("8/2/2022", item2.date());
        assertEquals("Marianela Montalvo", item2.clientName());
        assertEquals("496825", item2.accountNumber());
        assertEquals(AccountType.SAVINGS, item2.accountType());
        assertEquals(new BigDecimal("540.00"), item2.initialBalance());
        assertTrue(item2.status());
        assertEquals(new BigDecimal("-540.00"), item2.amount());
        assertEquals(BigDecimal.ZERO, item2.resultingBalance());
    }

    @Test
    @DisplayName("generateAccountStatement reports initial balance when account has no movements up to fechaFin")
    void generateAccountStatement_shouldReportInitialBalance_whenNoMovementsUpToEndDate() {
        Long clientId = 1L;
        LocalDate startDate = LocalDate.of(2022, 1, 1);
        LocalDate endDate = LocalDate.of(2022, 1, 31);

        ClientProjection client = new ClientProjection();
        client.setClientId(clientId);
        client.setName("Jose Lema");

        // Current balance already reflects a withdrawal made after the requested period
        Account account = new Account();
        account.setId(1L);
        account.setAccountNumber("478758");
        account.setAccountType(AccountType.SAVINGS);
        account.setInitialBalance(new BigDecimal("2000.00"));
        account.setCurrentBalance(new BigDecimal("1425.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setClient(client);

        when(clientProjectionRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(accountRepository.findByClient_ClientIdOrderById(clientId)).thenReturn(List.of(account));
        when(movementRepository.findClientMovementsBetween(eq(clientId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(movementRepository.findLastMovementsUpTo(List.of(1L), endDate.atTime(LocalTime.MAX)))
                .thenReturn(Collections.emptyList());

        List<AccountStatementReportItem> report = reportService.generateAccountStatement(
                clientId, startDate, endDate);

        assertEquals(1, report.size());
        AccountStatementReportItem item = report.get(0);
        assertEquals("31/1/2022", item.date());
        assertEquals("Jose Lema", item.clientName());
        assertEquals("478758", item.accountNumber());
        assertEquals(AccountType.SAVINGS, item.accountType());
        assertEquals(new BigDecimal("2000.00"), item.initialBalance());
        assertEquals(BigDecimal.ZERO, item.amount());
        assertEquals(new BigDecimal("2000.00"), item.resultingBalance());
    }

    @Test
    @DisplayName("generateAccountStatement reports balance of last movement before the period when range has no movements")
    void generateAccountStatement_shouldReportLastKnownBalance_whenNoMovementsInRange() {
        Long clientId = 3L;
        LocalDate startDate = LocalDate.of(2022, 3, 1);
        LocalDate endDate = LocalDate.of(2022, 3, 31);

        ClientProjection client = new ClientProjection();
        client.setClientId(clientId);
        client.setName("Juan Osorio");

        Account account = new Account();
        account.setId(30L);
        account.setAccountNumber("495878");
        account.setAccountType(AccountType.SAVINGS);
        account.setInitialBalance(BigDecimal.ZERO);
        account.setCurrentBalance(new BigDecimal("400.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setClient(client);

        Movement previousDeposit = new Movement();
        previousDeposit.setId(300L);
        previousDeposit.setAccount(account);
        previousDeposit.setMovementDate(LocalDateTime.of(2022, 2, 15, 12, 0));
        previousDeposit.setAmount(new BigDecimal("150.00"));
        previousDeposit.setResultingBalance(new BigDecimal("150.00"));
        previousDeposit.setMovementType(MovementType.INCOME);

        when(clientProjectionRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(accountRepository.findByClient_ClientIdOrderById(clientId)).thenReturn(List.of(account));
        when(movementRepository.findClientMovementsBetween(eq(clientId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(movementRepository.findLastMovementsUpTo(eq(List.of(30L)), any()))
                .thenReturn(List.of(previousDeposit));

        List<AccountStatementReportItem> report = reportService.generateAccountStatement(
                clientId, startDate, endDate);

        assertEquals(1, report.size());
        assertEquals(BigDecimal.ZERO, report.get(0).amount());
        assertEquals(new BigDecimal("150.00"), report.get(0).resultingBalance());
    }

    @Test
    @DisplayName("generateAccountStatement throws ClientProjectionNotFoundException when client not found")
    void generateAccountStatement_shouldThrow_whenClientNotFound() {
        when(clientProjectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClientProjectionNotFoundException.class, () ->
                reportService.generateAccountStatement(
                        99L, LocalDate.of(2022, 1, 1), LocalDate.of(2022, 1, 31)));
    }
}
