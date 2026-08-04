package com.eventdrivencommerce.inventory.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
