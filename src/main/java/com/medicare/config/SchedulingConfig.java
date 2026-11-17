package com.medicare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * @EnableScheduling switches on Spring's timer support - without it,
 * @Scheduled methods are silently ignored.
 *
 * The Clock bean is the application's single source of "what time is
 * it, and in which time zone". Appointment dates/times are stored
 * without a zone, so reminder logic must decide which zone they mean:
 * app.timezone (default Asia/Kolkata), NOT whatever the server's
 * operating-system zone happens to be. Injecting a Clock instead of
 * calling LocalDateTime.now() directly also makes the time-dependent
 * reminder rules testable with a fixed, fake clock.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Kolkata}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
