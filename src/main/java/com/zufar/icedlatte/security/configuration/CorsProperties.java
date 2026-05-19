package com.zufar.icedlatte.security.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(

        @NotEmpty(message = "CORS allowed-origins must not be empty")
        List<String> allowedOrigins,

        @NotEmpty(message = "CORS allowed-methods must not be empty")
        List<String> allowedMethods,

        @NotNull(message = "CORS allowed-headers must not be null")
        List<String> allowedHeaders,

        @NotNull(message = "CORS exposed-headers must not be null")
        List<String> exposedHeaders,

        @NotNull(message = "CORS allow-credentials must not be null")
        Boolean allowCredentials,

        @NotNull(message = "CORS max-age must not be null")
        Long maxAge
) {
}
