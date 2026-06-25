package com.zufar.icedlatte.user.config;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "avatar")
public record AvatarUploadProperties(
        @NotNull @DefaultValue("BACKEND") AvatarUploadMode uploadMode,
        @NotNull @DefaultValue("5m") Duration presignedUrlTtl,

        @Min(value = 1, message = "avatar.max-bytes must be positive") @DefaultValue("5242880")
        long maxBytes,

        @Min(value = 1, message = "avatar.max-pixels must be positive") @DefaultValue("12000000")
        long maxPixels,

        @NotNull @DefaultValue("10m") Duration processingTimeout,

        @NotBlank(message = "avatar.incoming-bucket must not be blank") @DefaultValue("iced-latte-users")
        String incomingBucket,

        @NotBlank(message = "avatar.processed-bucket must not be blank") @DefaultValue("iced-latte-users")
        String processedBucket,

        @DefaultValue("") String completionQueueUrl) {}
