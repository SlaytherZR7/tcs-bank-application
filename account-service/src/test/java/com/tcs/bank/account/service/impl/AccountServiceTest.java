package com.tcs.bank.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.bank.account.mappers.AccountMapper;
import com.tcs.bank.account.model.dto.AccountResponse;
import com.tcs.bank.account.model.dto.CreateAccountRequest;
import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import com.tcs.bank.account.model.enumeration.AccountType;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import com.tcs.bank.account.repository.AccountRepository;
import com.tcs.bank.account.repository.ClientProjectionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClientProjectionRepository clientProjectionRepository;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        AccountMapper mapper = Mappers.getMapper(AccountMapper.class);
        accountService = new AccountServiceImpl(accountRepository, clientProjectionRepository, mapper);
    }

    @Test
    @DisplayName("create initializes current balance and associates an active projected client")
    void create_shouldInitializeBalanceAndAssociateClient_whenClientIsActive() {
        // Arrange
        CreateAccountRequest request = new CreateAccountRequest(
                "  500001  ", 15L, AccountType.SAVINGS, new BigDecimal("125.50"), true);
        ClientProjection client = new ClientProjection();
        client.setClientId(15L);
        client.setName("Ana Pérez");
        client.setStatus(ClientStatus.ACTIVE);
        when(accountRepository.existsByAccountNumber("500001")).thenReturn(false);
        when(clientProjectionRepository.findById(15L)).thenReturn(Optional.of(client));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(2L);
            return account;
        });

        // Act
        AccountResponse response = accountService.create(request);

        // Assert
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account savedAccount = captor.getValue();
        assertEquals(new BigDecimal("125.50"), savedAccount.getInitialBalance());
        assertEquals(new BigDecimal("125.50"), savedAccount.getCurrentBalance());
        assertEquals(15L, savedAccount.getClient().getClientId());
        assertEquals(AccountStatus.ACTIVE, savedAccount.getStatus());
        assertEquals("Ana Pérez", response.clientName());
    }
}
