package com.zufar.icedlatte.user.config;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "avatar.completion-queue")
public record AvatarUploadCompletionQueueProperties(
        @DefaultValue("false") boolean enabled,

        @Min(value = 1, message = "avatar.completion-queue.max-messages must be at least 1")
        @Max(value = 10, message = "avatar.completion-queue.max-messages must be at most 10")
        @DefaultValue("5")
        int maxMessages,

        @NotNull @DefaultValue("30s") Duration visibilityTimeout,

        @NotNull @DefaultValue("1s") Duration waitTime) {}
