package com.tcs.bank.account.messaging;

import java.time.Instant;

public record ClientLifecycleEvent(
        String eventId,
        int schemaVersion,
        String eventType,
        Long clientId,
        String clientName,
        String clientStatus,
        Instant occurredAt) {
}
