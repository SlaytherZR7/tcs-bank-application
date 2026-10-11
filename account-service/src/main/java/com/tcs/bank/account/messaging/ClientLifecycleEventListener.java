package com.tcs.bank.account.messaging;

import com.tcs.bank.account.service.ClientProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("!test")
public class ClientLifecycleEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ClientLifecycleEventListener.class);

    private final ObjectMapper objectMapper;
    private final ClientProjectionService clientProjectionService;

    public ClientLifecycleEventListener(
            ObjectMapper objectMapper, ClientProjectionService clientProjectionService) {
        this.objectMapper = objectMapper;
        this.clientProjectionService = clientProjectionService;
    }

    @RabbitListener(queues = AccountMessagingConfiguration.CLIENT_EVENTS_QUEUE)
    public void receive(String payload) {
        ClientLifecycleEvent event;
        try {
            event = objectMapper.readValue(payload, ClientLifecycleEvent.class);
        } catch (RuntimeException exception) {
            logger.error("Rejecting unreadable client lifecycle event; routing it to the dead letter queue", exception);
            throw new AmqpRejectAndDontRequeueException("Invalid client lifecycle event", exception);
        }
        try {
            clientProjectionService.apply(event);
        } catch (IllegalArgumentException exception) {
            logger.error("Rejecting unsupported client lifecycle event {}; routing it to the dead letter queue",
                    event.eventId(), exception);
            throw new AmqpRejectAndDontRequeueException("Unsupported client lifecycle event", exception);
        }
    }
}
