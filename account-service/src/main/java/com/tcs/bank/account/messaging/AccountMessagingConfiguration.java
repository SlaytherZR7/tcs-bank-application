package com.tcs.bank.account.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
public class AccountMessagingConfiguration {

    public static final String CLIENT_EVENTS_EXCHANGE = "bank.client.events";
    public static final String CLIENT_EVENTS_ROUTING_KEY = "client.lifecycle.v1";
    public static final String CLIENT_EVENTS_QUEUE = "account.client-lifecycle.v1";
    public static final String DEAD_LETTER_EXCHANGE = "bank.account.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "account.client-lifecycle.dlq";
    private static final String DEAD_LETTER_ROUTING_KEY = "client.lifecycle.failed";

    @Bean
    TopicExchange clientEventsExchange() {
        return new TopicExchange(CLIENT_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue clientEventsQueue() {
        return QueueBuilder.durable(CLIENT_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue clientEventsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding bindClientEvents(
            @Qualifier("clientEventsQueue") Queue clientEventsQueue,
            @Qualifier("clientEventsExchange") TopicExchange clientEventsExchange) {
        return BindingBuilder.bind(clientEventsQueue)
                .to(clientEventsExchange)
                .with(CLIENT_EVENTS_ROUTING_KEY);
    }

    @Bean
    Binding bindClientEventsDeadLetterQueue(
            @Qualifier("clientEventsDeadLetterQueue") Queue clientEventsDeadLetterQueue,
            @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(clientEventsDeadLetterQueue)
                .to(deadLetterExchange)
                .with(DEAD_LETTER_ROUTING_KEY);
    }
}
