package com.tcs.bank.account.service.impl;

import com.tcs.bank.account.exceptions.ClientProjectionNotFoundException;
import com.tcs.bank.account.model.dto.AccountStatementReportItem;
import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.entity.Movement;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import com.tcs.bank.account.model.enumeration.AccountType;
import com.tcs.bank.account.repository.AccountRepository;
import com.tcs.bank.account.repository.ClientProjectionRepository;
import com.tcs.bank.account.repository.MovementRepository;
import com.tcs.bank.account.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

    private final ClientProjectionRepository clientProjectionRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    public ReportServiceImpl(
            ClientProjectionRepository clientProjectionRepository,
            AccountRepository accountRepository,
            MovementRepository movementRepository) {
        this.clientProjectionRepository = clientProjectionRepository;
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    public List<AccountStatementReportItem> generateAccountStatement(
            Long clientId, LocalDate startDate, LocalDate endDate) {

        ClientProjection client = clientProjectionRepository.findById(clientId)
                .orElseThrow(() -> new ClientProjectionNotFoundException(clientId));

        LocalDateTime fromDateTime = startDate.atStartOfDay();
        LocalDateTime toDateTime = endDate.atTime(LocalTime.MAX);

        List<Account> accounts = accountRepository.findByClient_ClientIdOrderById(clientId);

        Map<Long, List<Movement>> movementsByAccount = movementRepository
                .findClientMovementsBetween(clientId, fromDateTime, toDateTime)
                .stream()
                .collect(Collectors.groupingBy(movement -> movement.getAccount().getId()));

        Map<Long, BigDecimal> closingBalances = closingBalances(accounts, movementsByAccount, toDateTime);

        List<AccountStatementReportItem> reportItems = new ArrayList<>();

        for (Account account : accounts) {
            List<Movement> movements = movementsByAccount.getOrDefault(account.getId(), List.of());

            boolean isActive = account.getStatus() == AccountStatus.ACTIVE;
            AccountType accountType = account.getAccountType();

            if (movements.isEmpty()) {
                reportItems.add(new AccountStatementReportItem(
                        endDate.format(DATE_FORMATTER),
                        client.getName(),
                        account.getAccountNumber(),
                        accountType,
                        account.getInitialBalance(),
                        isActive,
                        BigDecimal.ZERO,
                        closingBalances.getOrDefault(account.getId(), account.getInitialBalance())));
            } else {
                for (Movement movement : movements) {
                    reportItems.add(new AccountStatementReportItem(
                            movement.getMovementDate().format(DATE_FORMATTER),
                            client.getName(),
                            account.getAccountNumber(),
                            accountType,
                            account.getInitialBalance(),
                            isActive,
                            movement.getAmount(),
                            movement.getResultingBalance()));
                }
            }
        }

        return reportItems;
    }

    private Map<Long, BigDecimal> closingBalances(
            List<Account> accounts, Map<Long, List<Movement>> movementsByAccount, LocalDateTime toDateTime) {
        List<Long> idleAccountIds = accounts.stream()
                .map(Account::getId)
                .filter(accountId -> !movementsByAccount.containsKey(accountId))
                .toList();

        if (idleAccountIds.isEmpty()) {
            return Map.of();
        }

        return movementRepository.findLastMovementsUpTo(idleAccountIds, toDateTime).stream()
                .collect(Collectors.toMap(
                        movement -> movement.getAccount().getId(),
                        Movement::getResultingBalance));
    }
}
