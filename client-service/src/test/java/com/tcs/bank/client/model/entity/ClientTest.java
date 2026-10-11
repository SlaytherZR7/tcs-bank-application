package com.tcs.bank.client.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.tcs.bank.client.model.enumeration.ClientStatus;
import com.tcs.bank.client.model.enumeration.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientTest {

    @Test
    @DisplayName("a new client starts as active")
    void constructor_shouldStartAsActive_whenClientIsCreated() {
        // Act
        Client client = new Client();

        // Assert
        assertEquals(ClientStatus.ACTIVE, client.getStatus());
    }

    @Test
    @DisplayName("markDeleted changes the client status to deleted")
    void markDeleted_shouldSetDeletedStatus_whenClientIsActive() {
        // Arrange
        Client client = new Client();

        // Act
        client.markDeleted();

        // Assert
        assertEquals(ClientStatus.DELETED, client.getStatus());
    }

    @Test
    @DisplayName("markDeleted keeps the client deleted when called again")
    void markDeleted_shouldKeepDeletedStatus_whenClientIsAlreadyDeleted() {
        // Arrange
        Client client = new Client();
        client.markDeleted();

        // Act
        client.markDeleted();

        // Assert
        assertEquals(ClientStatus.DELETED, client.getStatus());
    }

    @Test
    @DisplayName("markDeleted also removes an inactive client")
    void markDeleted_shouldSetDeletedStatus_whenClientIsInactive() {
        // Arrange
        Client client = new Client();
        client.setStatus(ClientStatus.INACTIVE);

        // Act
        client.markDeleted();

        // Assert
        assertEquals(ClientStatus.DELETED, client.getStatus());
    }

    @Test
    @DisplayName("a client inherits the personal data of Person")
    void client_shouldExposePersonData_whenPersonFieldsAreSet() {
        // Arrange
        Client client = new Client();

        // Act
        client.setName("Jose Lema");
        client.setGender(Gender.MALE);
        client.setAge(35);
        client.setIdentification("1712345678");
        client.setAddress("Otavalo sn y principal");
        client.setPhone("098254785");

        // Assert
        assertInstanceOf(Person.class, client);
        assertEquals("Jose Lema", client.getName());
        assertEquals(Gender.MALE, client.getGender());
        assertEquals(35, client.getAge());
        assertEquals("1712345678", client.getIdentification());
        assertEquals("Otavalo sn y principal", client.getAddress());
        assertEquals("098254785", client.getPhone());
    }
}
