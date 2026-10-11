package com.tcs.bank.account.messaging;

import com.tcs.bank.account.service.ClientProjectionService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("!test")
public class ClientLifecycleEventListener {

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
            throw new AmqpRejectAndDontRequeueException("Invalid client lifecycle event", exception);
        }
        try {
            clientProjectionService.apply(event);
        } catch (IllegalArgumentException exception) {
            throw new AmqpRejectAndDontRequeueException("Unsupported client lifecycle event", exception);
        }
    }
}
