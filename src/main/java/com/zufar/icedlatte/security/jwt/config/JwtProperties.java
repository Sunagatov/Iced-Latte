package com.zufar.icedlatte.security.jwt.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank(message = "JWT header cannot be blank") String header,

        @NotBlank(message = "JWT secret cannot be blank") String secret,
        // amazonq-ignore-next-line

        @NotBlank(message = "JWT refresh secret cannot be blank")
        String refreshSecret,

        @NotNull(message = "JWT expiration cannot be null") Duration expiration,

        @NotNull(message = "JWT refresh expiration cannot be null")
        Duration refreshExpiration,

        @NotBlank(message = "JWT issuer cannot be blank") String issuer,

        @NotBlank(message = "JWT audience cannot be blank") String audience) {

    public JwtProperties {
        if (expiration != null && (expiration.isZero() || expiration.isNegative())) {
            throw new IllegalArgumentException("jwt.expiration must be positive");
        }
        if (refreshExpiration != null && (refreshExpiration.isZero() || refreshExpiration.isNegative())) {
            throw new IllegalArgumentException("jwt.refresh-expiration must be positive");
        }
    }
}
