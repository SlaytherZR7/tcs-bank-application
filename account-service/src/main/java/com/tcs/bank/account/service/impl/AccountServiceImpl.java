package com.tcs.bank.account.service.impl;

import com.tcs.bank.account.exceptions.AccountNotFoundException;
import com.tcs.bank.account.exceptions.ClientNotActiveException;
import com.tcs.bank.account.exceptions.ClientProjectionNotFoundException;
import com.tcs.bank.account.exceptions.DuplicateAccountNumberException;
import com.tcs.bank.account.mappers.AccountMapper;
import com.tcs.bank.account.model.dto.AccountResponse;
import com.tcs.bank.account.model.dto.CreateAccountRequest;
import com.tcs.bank.account.model.dto.PatchAccountRequest;
import com.tcs.bank.account.model.dto.UpdateAccountRequest;
import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import com.tcs.bank.account.repository.AccountRepository;
import com.tcs.bank.account.repository.ClientProjectionRepository;
import com.tcs.bank.account.service.AccountService;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final ClientProjectionRepository clientProjectionRepository;
    private final AccountMapper accountMapper;

    public AccountServiceImpl(
            AccountRepository accountRepository,
            ClientProjectionRepository clientProjectionRepository,
            AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.clientProjectionRepository = clientProjectionRepository;
        this.accountMapper = accountMapper;
    }

    @Override
    public List<AccountResponse> findAll(Long clientId) {
        List<Account> accounts = clientId == null
                ? accountRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                : accountRepository.findByClient_ClientIdOrderById(clientId);
        return accounts.stream().map(accountMapper::toResponse).toList();
    }

    @Override
    public AccountResponse findById(Long accountId) {
        return accountMapper.toResponse(getAccount(accountId));
    }

    @Override
    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Account account = accountMapper.toEntity(request);
        ensureAccountNumberAvailable(account.getAccountNumber(), null);
        account.setClient(getActiveClient(request.clientId()));
        account.setCurrentBalance(account.getInitialBalance());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse replace(Long accountId, UpdateAccountRequest request) {
        Account account = getAccount(accountId);
        accountMapper.replace(account, request);
        ensureAccountNumberAvailable(account.getAccountNumber(), accountId);
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse patch(Long accountId, PatchAccountRequest request) {
        Account account = getAccount(accountId);
        accountMapper.patch(request, account);
        if (request.accountNumber() != null) {
            ensureAccountNumberAvailable(account.getAccountNumber(), accountId);
        }
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public void softDelete(Long accountId) {
        Account account = getAccount(accountId);
        if (account.getStatus() != AccountStatus.DELETED) {
            account.markDeleted();
            accountRepository.save(account);
        }
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private ClientProjection getActiveClient(Long clientId) {
        ClientProjection client = clientProjectionRepository.findById(clientId)
                .orElseThrow(() -> new ClientProjectionNotFoundException(clientId));
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new ClientNotActiveException(clientId);
        }
        return client;
    }

    private void ensureAccountNumberAvailable(String accountNumber, Long currentAccountId) {
        boolean accountNumberAlreadyUsed = currentAccountId == null
                ? accountRepository.existsByAccountNumber(accountNumber)
                : accountRepository.existsByAccountNumberAndIdNot(accountNumber, currentAccountId);
        if (accountNumberAlreadyUsed) {
            throw new DuplicateAccountNumberException();
        }
    }
}
