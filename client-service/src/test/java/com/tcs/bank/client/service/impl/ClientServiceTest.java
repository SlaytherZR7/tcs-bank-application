package com.tcs.bank.client.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.bank.client.mappers.ClientMapper;
import com.tcs.bank.client.messaging.ClientLifecycleEvent;
import com.tcs.bank.client.model.dto.ClientRequest;
import com.tcs.bank.client.model.dto.ClientResponse;
import com.tcs.bank.client.model.entity.Client;
import com.tcs.bank.client.model.enumeration.ClientStatus;
import com.tcs.bank.client.model.enumeration.Gender;
import com.tcs.bank.client.repository.ClientRepository;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ClientServiceImpl clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientServiceImpl(
                clientRepository, Mappers.getMapper(ClientMapper.class), passwordEncoder, eventPublisher);
    }

    @Test
    @DisplayName("create stores an encoded password and does not expose it in the response")
    void create_shouldEncodePasswordBeforeSaving_whenClientIsNew() {
        // Arrange
        ClientRequest request = new ClientRequest(
                "Ana Pérez", Gender.FEMALE, 32, " ID-100 ", "Quito", "0990000000", "1234", true);
        when(clientRepository.existsByIdentification("ID-100")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("$2a$encoded-hash");
        when(clientRepository.save(any(Client.class)))
                .thenAnswer(
                        invocation -> {
                            Client client = invocation.getArgument(0);
                            client.setId(1L);
                            return client;
                        });

        // Act
        ClientResponse response = clientService.create(request);

        // Assert
        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        verify(eventPublisher).publishEvent(any(ClientLifecycleEvent.class));
        assertEquals("$2a$encoded-hash", captor.getValue().getPasswordHash());
        assertEquals("ID-100", captor.getValue().getIdentification());
        assertEquals(ClientStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(1L, response.clientId());
        assertFalse(response.toString().contains("1234"));
    }
}
