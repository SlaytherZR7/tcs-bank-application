package com.tcs.bank.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.bank.account.exceptions.AccountNotActiveException;
import com.tcs.bank.account.exceptions.AccountNotFoundException;
import com.tcs.bank.account.exceptions.InsufficientFundsException;
import com.tcs.bank.account.exceptions.MovementAlreadyCorrectedException;
import com.tcs.bank.account.exceptions.MovementNotFoundException;
import com.tcs.bank.account.mappers.MovementMapper;
import com.tcs.bank.account.model.dto.CreateMovementRequest;
import com.tcs.bank.account.model.dto.MovementResponse;
import com.tcs.bank.account.model.dto.UpdateMovementRequest;
import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.entity.Movement;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import com.tcs.bank.account.model.enumeration.MovementType;
import com.tcs.bank.account.repository.AccountRepository;
import com.tcs.bank.account.repository.MovementRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private AccountRepository accountRepository;

    private MovementServiceImpl movementService;

    @BeforeEach
    void setUp() {
        MovementMapper mapper = Mappers.getMapper(MovementMapper.class);
        movementService = new MovementServiceImpl(movementRepository, accountRepository, mapper);
    }

    // ─── create ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create registers a deposit and increases the account balance")
    void create_shouldRegisterDeposit_whenAmountIsPositive() {
        Account account = activeAccount("225487", new BigDecimal("100.00"));
        when(accountRepository.findByAccountNumberWithLock("225487"))
                .thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(inv -> {
                    Movement m = inv.getArgument(0);
                    m.setId(1L);
                    return m;
                });
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateMovementRequest request = new CreateMovementRequest("225487", new BigDecimal("600.00"));

        MovementResponse response = movementService.create(request);

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepository).save(captor.capture());
        Movement saved = captor.getValue();

        assertEquals(new BigDecimal("600.00"), saved.getAmount());
        assertEquals(new BigDecimal("700.00"), saved.getResultingBalance());
        assertEquals(MovementType.INCOME, saved.getMovementType());
        assertEquals(MovementType.INCOME, response.movementType());
        assertEquals(new BigDecimal("700.00"), response.resultingBalance());
    }

    @Test
    @DisplayName("create registers a withdrawal and decreases the account balance")
    void create_shouldRegisterWithdrawal_whenAmountIsNegative() {
        Account account = activeAccount("478758", new BigDecimal("2000.00"));
        when(accountRepository.findByAccountNumberWithLock("478758"))
                .thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class)))
                .thenAnswer(inv -> {
                    Movement m = inv.getArgument(0);
                    m.setId(2L);
                    return m;
                });
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateMovementRequest request = new CreateMovementRequest("478758", new BigDecimal("-575.00"));

        MovementResponse response = movementService.create(request);

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepository).save(captor.capture());
        Movement saved = captor.getValue();

        assertEquals(new BigDecimal("-575.00"), saved.getAmount());
        assertEquals(new BigDecimal("1425.00"), saved.getResultingBalance());
        assertEquals(MovementType.EXPENSE, saved.getMovementType());
        assertEquals(MovementType.EXPENSE, response.movementType());
        assertEquals(new BigDecimal("1425.00"), response.resultingBalance());
    }

    @Test
    @DisplayName("create throws InsufficientFundsException when withdrawal exceeds balance")
    void create_shouldThrowInsufficientFunds_whenBalanceTooLow() {
        Account account = activeAccount("496825", new BigDecimal("540.00"));
        when(accountRepository.findByAccountNumberWithLock("496825"))
                .thenReturn(Optional.of(account));

        CreateMovementRequest request = new CreateMovementRequest("496825", new BigDecimal("-541.00"));

        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> movementService.create(request));

        assertEquals("Saldo no disponible", ex.getMessage());
    }

    @Test
    @DisplayName("create throws IllegalArgumentException when amount is zero")
    void create_shouldThrowIllegalArgument_whenAmountIsZero() {
        Account account = activeAccount("225487", new BigDecimal("100.00"));
        when(accountRepository.findByAccountNumberWithLock("225487"))
                .thenReturn(Optional.of(account));

        CreateMovementRequest request = new CreateMovementRequest("225487", BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> movementService.create(request));
    }

    @Test
    @DisplayName("create throws AccountNotFoundException when account number is unknown")
    void create_shouldThrow_whenAccountNotFound() {
        when(accountRepository.findByAccountNumberWithLock("000000"))
                .thenReturn(Optional.empty());

        CreateMovementRequest request = new CreateMovementRequest("000000", new BigDecimal("100.00"));

        assertThrows(AccountNotFoundException.class, () -> movementService.create(request));
    }

    @Test
    @DisplayName("create throws AccountNotActiveException when account is INACTIVE")
    void create_shouldThrow_whenAccountInactive() {
        Account account = activeAccount("495878", BigDecimal.ZERO);
        account.setStatus(AccountStatus.INACTIVE);
        when(accountRepository.findByAccountNumberWithLock("495878"))
                .thenReturn(Optional.of(account));

        CreateMovementRequest request = new CreateMovementRequest("495878", new BigDecimal("150.00"));

        assertThrows(AccountNotActiveException.class, () -> movementService.create(request));
    }

    // ─── replace (PUT) ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("replace registers a reversal and replacement without modifying historical movements")
    void replace_shouldReverseAndCreateNewMovement_deterministically() {
        Account account = activeAccount("478758", new BigDecimal("1425.00"));
        account.setId(1L);

        Movement original = movement(10L, account, new BigDecimal("-575.00"),
                new BigDecimal("1425.00"), LocalDateTime.now().minusMinutes(10));

        when(movementRepository.findById(10L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByCorrectionOf_Id(10L)).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("478758")).thenReturn(Optional.of(account));

        AtomicLong idSeq = new AtomicLong(100L);
        when(movementRepository.save(any(Movement.class))).thenAnswer(inv -> {
            Movement m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(idSeq.getAndIncrement());
            }
            return m;
        });
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateMovementRequest request = new UpdateMovementRequest(new BigDecimal("-300.00"));

        MovementResponse response = movementService.replace(10L, request);

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepository, times(2)).save(captor.capture());
        List<Movement> savedMovements = captor.getAllValues();

        // Reversal: +575, correctionOf = original
        Movement reversal = savedMovements.get(0);
        assertEquals(new BigDecimal("575.00"), reversal.getAmount());
        assertEquals(new BigDecimal("2000.00"), reversal.getResultingBalance());
        assertSame(original, reversal.getCorrectionOf());

        // Replacement: -300, correctionOf = original
        Movement replacement = savedMovements.get(1);
        assertEquals(new BigDecimal("-300.00"), replacement.getAmount());
        assertEquals(new BigDecimal("1700.00"), replacement.getResultingBalance());
        assertSame(original, replacement.getCorrectionOf());

        assertEquals(new BigDecimal("1700.00"), response.resultingBalance());
        assertEquals(new BigDecimal("1700.00"), account.getCurrentBalance());
    }

    @Test
    @DisplayName("replace throws MovementAlreadyCorrectedException when movement was already corrected")
    void replace_shouldThrow_whenMovementAlreadyCorrected() {
        Movement original = movement(10L, activeAccount("478758", new BigDecimal("1425.00")),
                new BigDecimal("-575.00"), new BigDecimal("1425.00"), LocalDateTime.now());

        when(movementRepository.findById(10L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByCorrectionOf_Id(10L)).thenReturn(true);

        UpdateMovementRequest request = new UpdateMovementRequest(new BigDecimal("-300.00"));

        assertThrows(MovementAlreadyCorrectedException.class, () -> movementService.replace(10L, request));
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace throws InsufficientFundsException when reverting deposit leaves balance negative")
    void replace_shouldThrow_whenRevertingLeavesNegativeBalance() {
        Account account = activeAccount("225487", new BigDecimal("30.00"));
        Movement deposit = movement(1L, account, new BigDecimal("100.00"),
                new BigDecimal("100.00"), LocalDateTime.now().minusDays(1));

        when(movementRepository.findById(1L)).thenReturn(Optional.of(deposit));
        when(movementRepository.existsByCorrectionOf_Id(1L)).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("225487")).thenReturn(Optional.of(account));

        UpdateMovementRequest request = new UpdateMovementRequest(new BigDecimal("120.00"));

        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> movementService.replace(1L, request));

        assertEquals("Saldo no disponible", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("replace throws InsufficientFundsException when replacement leaves balance negative")
    void replace_shouldThrow_whenReplacementLeavesNegativeBalance() {
        Account account = activeAccount("478758", new BigDecimal("100.00"));
        Movement withdrawal = movement(2L, account, new BigDecimal("-50.00"),
                new BigDecimal("100.00"), LocalDateTime.now().minusHours(2));

        when(movementRepository.findById(2L)).thenReturn(Optional.of(withdrawal));
        when(movementRepository.existsByCorrectionOf_Id(2L)).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("478758")).thenReturn(Optional.of(account));

        // balanceAfterRevert = 100 - (-50) = 150. New withdrawal = -200 -> final = -50 (fails)
        UpdateMovementRequest request = new UpdateMovementRequest(new BigDecimal("-200.00"));

        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> movementService.replace(2L, request));

        assertEquals("Saldo no disponible", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    // ─── compensate (DELETE) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("compensate registers a compensatory movement linked to original")
    void compensate_shouldCreateCompensatoryMovement() {
        Account account = activeAccount("496825", BigDecimal.ZERO);
        account.setId(2L);

        Movement original = movement(20L, account, new BigDecimal("-540.00"),
                BigDecimal.ZERO, LocalDateTime.now().minusMinutes(5));

        when(movementRepository.findById(20L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByCorrectionOf_Id(20L)).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("496825"))
                .thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        movementService.compensate(20L);

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepository).save(captor.capture());
        Movement compensatory = captor.getValue();
        assertEquals(new BigDecimal("540.00"), compensatory.getAmount());
        assertEquals(MovementType.INCOME, compensatory.getMovementType());
        assertEquals(new BigDecimal("540.00"), compensatory.getResultingBalance());
        assertSame(original, compensatory.getCorrectionOf());
    }

    @Test
    @DisplayName("compensate is idempotent: repeated call returns 204 without creating duplicate movement")
    void compensate_shouldBeIdempotent_whenCalledRepeatedly() {
        Movement original = movement(20L, activeAccount("496825", new BigDecimal("540.00")),
                new BigDecimal("-540.00"), BigDecimal.ZERO, LocalDateTime.now());

        when(movementRepository.findById(20L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByCorrectionOf_Id(20L)).thenReturn(true);

        movementService.compensate(20L);

        verify(movementRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("compensate throws InsufficientFundsException when compensating a deposit would leave balance negative")
    void compensate_shouldThrow_whenDepositAlreadySpent() {
        Account account = activeAccount("225487", new BigDecimal("40.00"));
        Movement deposit = movement(5L, account, new BigDecimal("100.00"),
                new BigDecimal("100.00"), LocalDateTime.now());

        when(movementRepository.findById(5L)).thenReturn(Optional.of(deposit));
        when(movementRepository.existsByCorrectionOf_Id(5L)).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("225487")).thenReturn(Optional.of(account));

        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> movementService.compensate(5L));

        assertEquals("Saldo no disponible", ex.getMessage());
        verify(movementRepository, never()).save(any());
    }

    @Test
    @DisplayName("compensate succeeds even if account is INACTIVE or DELETED")
    void compensate_shouldSucceed_evenIfAccountInactive() {
        Account account = activeAccount("496825", BigDecimal.ZERO);
        account.setStatus(AccountStatus.INACTIVE);

        Movement original = movement(20L, account, new BigDecimal("-540.00"),
                BigDecimal.ZERO, LocalDateTime.now().minusMinutes(5));

        when(movementRepository.findById(20L)).thenReturn(Optional.of(original));
        when(movementRepository.existsByCorrectionOf_Id(20L)).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("496825")).thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        movementService.compensate(20L);

        verify(movementRepository).save(any(Movement.class));
        assertEquals(new BigDecimal("540.00"), account.getCurrentBalance());
    }

    @Test
    @DisplayName("append-only ledger invariant holds with subsequent movements intact")
    void ledgerInvariantsHold_withSubsequentMovements() {
        // Scenario:
        // m1: Deposit +100 -> rb = 100
        // m2: Deposit +50  -> rb = 150 (subsequent to m1)
        // Current balance = 150
        Account account = activeAccount("225487", new BigDecimal("150.00"));
        account.setId(10L);

        Movement m1 = movement(1L, account, new BigDecimal("100.00"),
                new BigDecimal("100.00"), LocalDateTime.now().minusHours(2));
        Movement m2 = movement(2L, account, new BigDecimal("50.00"),
                new BigDecimal("150.00"), LocalDateTime.now().minusHours(1));

        // Anulate m1
        when(movementRepository.findById(1L)).thenReturn(Optional.of(m1));
        when(movementRepository.existsByCorrectionOf_Id(1L)).thenReturn(false);
        when(accountRepository.findByAccountNumberWithLock("225487")).thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        movementService.compensate(1L);

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepository).save(captor.capture());
        Movement compensatory = captor.getValue();

        // Compensatory is appended: -100, resulting in 50
        assertEquals(new BigDecimal("-100.00"), compensatory.getAmount());
        assertEquals(new BigDecimal("50.00"), compensatory.getResultingBalance());

        // Invariants:
        // 1. Account current balance == resultingBalance of latest movement
        assertEquals(account.getCurrentBalance(), compensatory.getResultingBalance());
        // 2. Historical movements m1 and m2 remained untouched
        assertEquals(new BigDecimal("100.00"), m1.getResultingBalance());
        assertEquals(new BigDecimal("150.00"), m2.getResultingBalance());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private Account activeAccount(String accountNumber, BigDecimal currentBalance) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setCurrentBalance(currentBalance);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private Movement movement(Long id, Account account, BigDecimal amount,
            BigDecimal resultingBalance, LocalDateTime date) {
        Movement m = new Movement();
        m.setId(id);
        m.setAccount(account);
        m.setAmount(amount);
        m.setResultingBalance(resultingBalance);
        m.setMovementDate(date);
        m.setMovementType(MovementType.fromAmount(amount));
        return m;
    }
}
