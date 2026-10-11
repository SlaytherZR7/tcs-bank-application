package com.tcs.bank.client.messaging;

import java.time.Instant;

public record ClientLifecycleEvent(
        String eventId,
        int schemaVersion,
        String eventType,
        Long clientId,
        String clientName,
        String clientStatus,
        Instant occurredAt) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CLIENT_CREATED = "ClientCreated";
    public static final String CLIENT_UPDATED = "ClientUpdated";
    public static final String CLIENT_DEACTIVATED = "ClientDeactivated";
}
