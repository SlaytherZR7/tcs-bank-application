package com.tcs.bank.client.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcs.bank.client.model.enumeration.ClientStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientTest {

    @Test
    @DisplayName("markDeleted changes the client status to deleted")
    void markDeleted_shouldSetDeletedStatus() {
        // Arrange
        Client client = new Client();
        assertEquals(ClientStatus.ACTIVE, client.getStatus());

        // Act
        client.markDeleted();

        // Assert
        assertEquals(ClientStatus.DELETED, client.getStatus());
    }
}
