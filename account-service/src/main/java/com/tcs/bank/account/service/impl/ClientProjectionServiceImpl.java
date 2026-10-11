package com.tcs.bank.account.service.impl;

import com.tcs.bank.account.messaging.ClientLifecycleEvent;
import com.tcs.bank.account.model.entity.ClientProjection;
import com.tcs.bank.account.model.enumeration.ClientStatus;
import com.tcs.bank.account.repository.ClientProjectionRepository;
import com.tcs.bank.account.service.ClientProjectionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientProjectionServiceImpl implements ClientProjectionService {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ClientProjectionRepository clientProjectionRepository;

    public ClientProjectionServiceImpl(ClientProjectionRepository clientProjectionRepository) {
        this.clientProjectionRepository = clientProjectionRepository;
    }

    @Override
    @Transactional
    public void apply(ClientLifecycleEvent event) {
        if (event.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported client event schema version: " + event.schemaVersion());
        }

        ClientProjection projection = clientProjectionRepository.findById(event.clientId())
                .orElseGet(ClientProjection::new);

        if (event.eventId().equals(projection.getLastEventId())) {
            return;
        }
        if (projection.getLastEventAt() != null
                && event.occurredAt().isBefore(projection.getLastEventAt())) {
            return;
        }

        projection.setClientId(event.clientId());
        projection.setName(event.clientName());
        projection.setStatus(ClientStatus.valueOf(event.clientStatus()));
        projection.setLastEventId(event.eventId());
        projection.setLastEventAt(event.occurredAt());
        clientProjectionRepository.save(projection);
    }
}
