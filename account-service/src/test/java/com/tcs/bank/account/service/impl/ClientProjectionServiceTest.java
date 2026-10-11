package com.tcs.bank.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.bank.account.messaging.ClientLifecycleEvent;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import com.tcs.bank.account.repository.ClientProjectionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientProjectionServiceTest {

    @Mock
    private ClientProjectionRepository clientProjectionRepository;

    private ClientProjectionServiceImpl clientProjectionService;

    @BeforeEach
    void setUp() {
        clientProjectionService = new ClientProjectionServiceImpl(clientProjectionRepository);
    }

    @Test
    @DisplayName("apply stores a client snapshot received from an event")
    void apply_shouldCreateProjectionFromEvent() {
        // Arrange
        ClientLifecycleEvent event = event("event-1", "ClientCreated", "ACTIVE", Instant.parse("2026-05-01T10:00:00Z"));
        when(clientProjectionRepository.findById(15L)).thenReturn(Optional.empty());
        when(clientProjectionRepository.save(any(ClientProjection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        clientProjectionService.apply(event);

        // Assert
        ArgumentCaptor<ClientProjection> captor = ArgumentCaptor.forClass(ClientProjection.class);
        verify(clientProjectionRepository).save(captor.capture());
        assertEquals("Ana Pérez", captor.getValue().getName());
        assertEquals(ClientStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals("event-1", captor.getValue().getLastEventId());
    }

    @Test
    @DisplayName("apply ignores duplicate event delivery")
    void apply_shouldIgnoreDuplicateEvent() {
        // Arrange
        ClientLifecycleEvent event = event("event-1", "ClientCreated", "ACTIVE", Instant.parse("2026-05-01T10:00:00Z"));
        ClientProjection projection = new ClientProjection();
        projection.setClientId(15L);
        projection.setName("Ana Pérez");
        projection.setStatus(ClientStatus.ACTIVE);
        projection.setLastEventId("event-1");
        projection.setLastEventAt(event.occurredAt());
        when(clientProjectionRepository.findById(15L)).thenReturn(Optional.of(projection));

        // Act
        clientProjectionService.apply(event);

        // Assert
        verify(clientProjectionRepository, never()).save(any(ClientProjection.class));
    }

    private ClientLifecycleEvent event(
            String eventId, String eventType, String status, Instant occurredAt) {
        return new ClientLifecycleEvent(eventId, 1, eventType, 15L, "Ana Pérez", status, occurredAt);
    }
}
