package com.zufar.onlinestore.reservation.config;

import java.time.Instant;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "application.reservation")
public record ReservationTimeoutConfiguration(Duration defaultTimeout) {

    public Instant getExpiredAt(Instant createdAt) {
        return createdAt.plus(defaultTimeout);
    }
}