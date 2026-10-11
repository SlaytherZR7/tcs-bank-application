package com.tcs.bank.account.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcs.bank.account.model.dto.CreateAccountRequest;
import com.tcs.bank.account.model.dto.PatchAccountRequest;
import com.tcs.bank.account.model.entity.Account;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.enumeration.AccountStatus;
import com.tcs.bank.account.model.enumeration.AccountType;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AccountMapperTest {

    private final AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);

    @Test
    @DisplayName("maps creation data and converts the public status to the internal enum")
    void toEntity_shouldMapCreationRequest() {
        // Arrange
        CreateAccountRequest request = new CreateAccountRequest(
                "  500001  ", 15L, AccountType.SAVINGS, new BigDecimal("125.50"), true);

        // Act
        Account account = accountMapper.toEntity(request);

        // Assert
        assertEquals("500001", account.getAccountNumber());
        assertEquals(AccountType.SAVINGS, account.getAccountType());
        assertEquals(new BigDecimal("125.50"), account.getInitialBalance());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    @DisplayName("patch preserves client and balances when those fields are not provided")
    void patch_shouldPreserveImmutableAndOmittedFields() {
        // Arrange
        Account account = new Account();
        account.setAccountNumber("500001");
        account.setInitialBalance(new BigDecimal("125.50"));
        account.setCurrentBalance(new BigDecimal("110.50"));
        ClientProjection client = new ClientProjection();
        client.setClientId(15L);
        client.setName("Ana Pérez");
        client.setStatus(ClientStatus.ACTIVE);
        account.setClient(client);
        PatchAccountRequest request = new PatchAccountRequest("  500002  ", null, null);

        // Act
        accountMapper.patch(request, account);

        // Assert
        assertEquals("500002", account.getAccountNumber());
        assertEquals(new BigDecimal("125.50"), account.getInitialBalance());
        assertEquals(new BigDecimal("110.50"), account.getCurrentBalance());
        assertEquals(15L, account.getClient().getClientId());
    }
}
