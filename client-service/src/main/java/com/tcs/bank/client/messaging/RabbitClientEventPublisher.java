package com.tcs.bank.client.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

@Component
public class RabbitClientEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitClientEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitClientEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ClientLifecycleEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(
                    ClientMessagingConfiguration.EXCHANGE_NAME,
                    ClientMessagingConfiguration.ROUTING_KEY,
                    payload);
            LOGGER.info("Published {} event {} for client {}", event.eventType(), event.eventId(), event.clientId());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Failed to publish client lifecycle event {} for client {}",
                    event.eventId(),
                    event.clientId(),
                    exception);
        }
    }
}
