package com.tcs.bank.account.service.impl;

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
import com.tcs.bank.account.service.MovementService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MovementServiceImpl implements MovementService {

    private static final Logger logger = LoggerFactory.getLogger(MovementServiceImpl.class);

    private final MovementRepository movementRepository;
    private final AccountRepository accountRepository;
    private final MovementMapper movementMapper;

    public MovementServiceImpl(
            MovementRepository movementRepository,
            AccountRepository accountRepository,
            MovementMapper movementMapper) {
        this.movementRepository = movementRepository;
        this.accountRepository = accountRepository;
        this.movementMapper = movementMapper;
    }

    @Override
    public List<MovementResponse> findAll(String accountNumber) {
        List<Movement> movements = accountNumber == null
                ? movementRepository.findAllByOrderByMovementDateAscIdAsc()
                : movementRepository.findByAccount_AccountNumberOrderByMovementDateAscIdAsc(accountNumber);
        return movements.stream().map(movementMapper::toResponse).toList();
    }

    @Override
    public MovementResponse findById(Long movementId) {
        return movementMapper.toResponse(getMovement(movementId));
    }

    @Override
    @Transactional
    public MovementResponse create(CreateMovementRequest request) {
        // 1. Resolve account with pessimistic lock to prevent concurrent balance races
        Account account = accountRepository
                .findByAccountNumberWithLock(request.accountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.accountNumber()));

        // 2. Account must be ACTIVE to accept transactions
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException(request.accountNumber());
        }

        BigDecimal amount = request.valor();

        // 3. Reject zero-amount movements
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("valor: no puede ser cero");
        }

        // 4. Validate sufficient funds for withdrawals
        BigDecimal newBalance = account.getCurrentBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Insufficient funds for account {}: current={}, requested={}",
                    request.accountNumber(), account.getCurrentBalance(), amount);
            throw new InsufficientFundsException();
        }

        // 5. Build and persist the movement
        Movement movement = buildMovement(account, amount, newBalance, LocalDateTime.now(), null);
        movementRepository.save(movement);

        // 6. Update the account balance
        account.setCurrentBalance(newBalance);
        accountRepository.save(account);

        logger.info("Movement created for account {}: amount={}, newBalance={}",
                request.accountNumber(), amount, newBalance);

        return movementMapper.toResponse(movement);
    }

    @Override
    @Transactional
    public MovementResponse replace(Long movementId, UpdateMovementRequest request) {
        Movement original = getMovement(movementId);

        // Disallow correcting a movement that was already corrected or compensated
        if (movementRepository.existsByCorrectionOf_Id(movementId)) {
            throw new MovementAlreadyCorrectedException(movementId);
        }

        Account account = accountRepository
                .findByAccountNumberWithLock(original.getAccount().getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(original.getAccount().getAccountNumber()));

        BigDecimal originalAmount = original.getAmount();
        BigDecimal newAmount = request.valor();

        // Reject zero-amount corrections
        if (newAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("valor: no puede ser cero");
        }

        // Validate balances before any persistence:
        // 1. Reversing the original movement must not produce a negative balance (e.g. deposited funds already spent)
        BigDecimal balanceAfterRevert = account.getCurrentBalance().subtract(originalAmount);
        if (balanceAfterRevert.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Cannot revert movement {} on account {}: currentBalance={}, originalAmount={}",
                    movementId, account.getAccountNumber(), account.getCurrentBalance(), originalAmount);
            throw new InsufficientFundsException();
        }

        // 2. Applying the new amount must not leave the account negative
        BigDecimal finalBalance = balanceAfterRevert.add(newAmount);
        if (finalBalance.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Insufficient funds after replacing movement {} on account {}: balanceAfterRevert={}, newAmount={}",
                    movementId, account.getAccountNumber(), balanceAfterRevert, newAmount);
            throw new InsufficientFundsException();
        }

        LocalDateTime now = LocalDateTime.now();

        // Append reversal movement linked to original
        BigDecimal reversalAmount = originalAmount.negate();
        Movement reversal = buildMovement(account, reversalAmount, balanceAfterRevert, now, original);
        movementRepository.save(reversal);

        // Append replacement movement linked to original
        Movement replacement = buildMovement(account, newAmount, finalBalance, now, original);
        movementRepository.save(replacement);

        // Update account balance
        account.setCurrentBalance(finalBalance);
        accountRepository.save(account);

        logger.info("Movement {} replaced on account {}: original={}, new={}, finalBalance={}",
                movementId, account.getAccountNumber(), originalAmount, newAmount, finalBalance);

        return movementMapper.toResponse(replacement);
    }

    @Override
    @Transactional
    public void compensate(Long movementId) {
        Movement original = getMovement(movementId);

        // Idempotent: if already compensated or corrected, return silently without creating another movement
        if (movementRepository.existsByCorrectionOf_Id(movementId)) {
            logger.info("Movement {} already corrected/compensated. Skipping duplicate compensatory movement.", movementId);
            return;
        }

        Account account = accountRepository
                .findByAccountNumberWithLock(original.getAccount().getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(original.getAccount().getAccountNumber()));

        BigDecimal originalAmount = original.getAmount();
        BigDecimal compensatoryAmount = originalAmount.negate();
        BigDecimal newBalance = account.getCurrentBalance().add(compensatoryAmount);

        // Validate that reversing does not breach zero balance constraint
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            logger.warn("Cannot compensate movement {} on account {}: currentBalance={}, compensatoryAmount={}",
                    movementId, account.getAccountNumber(), account.getCurrentBalance(), compensatoryAmount);
            throw new InsufficientFundsException();
        }

        Movement compensatory = buildMovement(account, compensatoryAmount, newBalance, LocalDateTime.now(), original);
        movementRepository.save(compensatory);

        account.setCurrentBalance(newBalance);
        accountRepository.save(account);

        logger.info("Movement {} compensated on account {}: compensatoryAmount={}, newBalance={}",
                movementId, account.getAccountNumber(), compensatoryAmount, newBalance);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Movement getMovement(Long movementId) {
        return movementRepository.findById(movementId)
                .orElseThrow(() -> new MovementNotFoundException(movementId));
    }

    private Movement buildMovement(
            Account account,
            BigDecimal amount,
            BigDecimal resultingBalance,
            LocalDateTime date,
            Movement correctionOf) {
        Movement movement = new Movement();
        movement.setAccount(account);
        movement.setAmount(amount);
        movement.setMovementType(MovementType.fromAmount(amount));
        movement.setResultingBalance(resultingBalance);
        movement.setMovementDate(date);
        movement.setCorrectionOf(correctionOf);
        return movement;
    }
}
