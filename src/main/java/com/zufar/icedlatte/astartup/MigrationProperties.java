package com.zufar.icedlatte.astartup;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "migration")
public record MigrationProperties(
        @DefaultValue Upload upload,
        @DefaultValue Ratings ratings,
        Duration timeout,
        @DefaultValue("5") Integer timeoutMinutes) {

    public MigrationProperties {
        upload = upload == null ? new Upload(false) : upload;
        ratings = ratings == null ? new Ratings(false) : ratings;
        timeout = timeout == null ? Duration.ofMinutes(timeoutMinutes == null ? 5 : timeoutMinutes) : timeout;
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("migration.timeout must be positive");
        }
    }

    public record Upload(@DefaultValue("false") boolean enabled) {}

    public record Ratings(@DefaultValue("false") boolean enabled) {}
}
