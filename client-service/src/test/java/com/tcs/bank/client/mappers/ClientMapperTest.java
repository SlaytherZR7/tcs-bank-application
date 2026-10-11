package com.tcs.bank.client.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tcs.bank.client.model.dto.PatchClientRequest;
import com.tcs.bank.client.model.entity.Client;
import com.tcs.bank.client.model.enumeration.ClientStatus;
import com.tcs.bank.client.model.enumeration.Gender;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClientMapperTest {

    private final ClientMapper clientMapper = Mappers.getMapper(ClientMapper.class);

    @ParameterizedTest
    @CsvSource({ "true, ACTIVE", "false, INACTIVE" })
    @DisplayName("maps the public boolean status to its internal lifecycle enum")
    void toClientStatus_shouldMapBooleanToLifecycleStatus(
            boolean active, ClientStatus expectedStatus) {
        // Arrange

        // Act
        ClientStatus status = clientMapper.toClientStatus(active);

        // Assert
        assertEquals(expectedStatus, status);
    }

    @Test
    @DisplayName("maps a deleted client to false in the legacy boolean response")
    void toResponse_shouldMapDeletedStatusToFalse() {
        // Arrange
        Client client = new Client();
        client.setStatus(ClientStatus.DELETED);

        // Act
        var response = clientMapper.toResponse(client);

        // Assert
        assertFalse(response.status());
    }

    @Test
    @DisplayName("patch maps only provided fields and preserves omitted values")
    void patch_shouldLeaveOmittedFieldsUnchanged() {
        // Arrange
        Client client = new Client();
        client.setName("Original name");
        client.setGender(Gender.FEMALE);
        client.setAge(30);
        PatchClientRequest request = new PatchClientRequest("  Updated name  ", null, null, null, null, null, null,
                null);

        // Act
        clientMapper.patch(request, client);

        // Assert
        assertEquals("Updated name", client.getName());
        assertEquals(Gender.FEMALE, client.getGender());
        assertEquals(30, client.getAge());
    }
}
