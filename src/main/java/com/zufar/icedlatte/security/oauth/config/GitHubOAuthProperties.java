package com.zufar.icedlatte.security.oauth.config;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "github")
public record GitHubOAuthProperties(
        @NotBlank(message = "GitHub client-id must not be blank")
        String clientId,

        @NotBlank(message = "GitHub client-secret must not be blank")
        String clientSecret,

        @NotBlank(message = "GitHub redirect-uri must not be blank")
        String redirectUri,

        @NotBlank(message = "GitHub scope must not be blank")
        String scope,

        @NotBlank(message = "GitHub API version must not be blank")
        String apiVersion,

        @Valid @NotNull(message = "GitHub timeout config must not be null")
        Timeout timeout) {

    public record Timeout(
            @NotNull(message = "GitHub connect-timeout must not be null")
            Duration connectTimeout,

            @NotNull(message = "GitHub read-timeout must not be null")
            Duration readTimeout) {}
}
