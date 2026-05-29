package com.zufar.icedlatte.filestorage.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "file-storage.deletion-outbox")
public record FileDeletionOutboxProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("true") boolean workerEnabled,

        @Min(value = 1, message = "file-storage.deletion-outbox.batch-size must be positive") @DefaultValue("25")
        int batchSize,

        @Min(value = 1, message = "file-storage.deletion-outbox.max-attempts must be positive") @DefaultValue("10")
        int maxAttempts,

        @NotNull(message = "file-storage.deletion-outbox.poll-interval must not be null") @DefaultValue("30s")
        Duration pollInterval,

        @NotNull(message = "file-storage.deletion-outbox.stale-lock-timeout must not be null") @DefaultValue("5m")
        Duration staleLockTimeout,

        @NotBlank(message = "file-storage.deletion-outbox.worker-id must not be blank")
        @DefaultValue("iced-latte-file-deletion-worker")
        String workerId) {}
