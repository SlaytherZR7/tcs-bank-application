package com.tcs.bank.account.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock(@Value("${bank.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
