package com.zufar.icedlatte.filestorage.aws;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.aws")
public record AwsProperties(
        @NotBlank(message = "AWS access-key must not be blank")
        String accessKey,

        @NotBlank(message = "AWS secret-key must not be blank")
        String secretKey,

        @NotBlank(message = "AWS region must not be blank") String region,

        String endpointUrl,

        @NotNull(message = "AWS read-timeout must not be null") @DefaultValue("10s")
        Duration readTimeout,

        @NotNull(message = "AWS connect-timeout must not be null") @DefaultValue("10s")
        Duration connectTimeout,

        @NotNull(message = "AWS write-timeout must not be null") @DefaultValue("60s")
        Duration writeTimeout,

        @NotNull(message = "AWS api-call-timeout must not be null") @DefaultValue("60s")
        Duration apiCallTimeout,

        @NotNull(message = "AWS api-call-attempt-timeout must not be null") @DefaultValue("15s")
        Duration apiCallAttemptTimeout) {}
