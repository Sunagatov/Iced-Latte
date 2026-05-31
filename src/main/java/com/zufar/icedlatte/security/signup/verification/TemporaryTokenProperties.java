package com.zufar.icedlatte.security.signup.verification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "temporary-cache")
public record TemporaryTokenProperties(@Valid @NotNull Time time) {

    public TemporaryTokenProperties {
        if (time == null) {
            throw new IllegalArgumentException("temporary-cache.time must not be null");
        }
    }

    public record Time(Integer token) {

        public Time {
            if (token == null) {
                throw new IllegalArgumentException("temporary-cache.time.token must not be null");
            }
            if (token < 1) {
                throw new IllegalArgumentException("temporary-cache.time.token must be at least 1 minute");
            }
        }
    }
}
