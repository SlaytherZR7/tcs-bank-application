package com.tcs.bank.account.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcs.bank.account.model.enumeration.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    @DisplayName("markDeleted moves an active account to deleted")
    void markDeleted_shouldSetDeletedStatus_whenAccountIsActive() {
        // Arrange
        Account account = new Account();
        assertEquals(AccountStatus.ACTIVE, account.getStatus());

        // Act
        account.markDeleted();

        // Assert
        assertEquals(AccountStatus.DELETED, account.getStatus());
    }
}
