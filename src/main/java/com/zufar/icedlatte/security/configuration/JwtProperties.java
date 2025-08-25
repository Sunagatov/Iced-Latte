package com.zufar.icedlatte.security.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

@Data
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "JWT secret cannot be blank")
    private String secret;

    @NotBlank(message = "JWT refresh secret cannot be blank") 
    private String refreshSecret;

    @NotNull(message = "JWT expiration cannot be null")
    private Duration expiration = Duration.ofHours(24);

    @NotNull(message = "JWT refresh expiration cannot be null")
    private Duration refreshExpiration = Duration.ofDays(7);

    @NotBlank(message = "JWT issuer cannot be blank")
    private String issuer = "iced-latte";

    @NotBlank(message = "JWT audience cannot be blank")
    private String audience = "iced-latte-users";
}