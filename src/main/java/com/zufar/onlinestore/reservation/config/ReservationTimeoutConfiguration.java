package com.zufar.onlinestore.reservation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "application.reservation")
public record ReservationTimeoutConfiguration(Duration defaultTimeout) {
}