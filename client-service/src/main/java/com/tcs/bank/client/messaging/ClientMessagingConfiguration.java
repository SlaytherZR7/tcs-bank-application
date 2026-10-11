package com.tcs.bank.client.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClientMessagingConfiguration {

    public static final String EXCHANGE_NAME = "bank.client.events";
    public static final String ROUTING_KEY = "client.lifecycle.v1";

    @Bean
    TopicExchange clientEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }
}
