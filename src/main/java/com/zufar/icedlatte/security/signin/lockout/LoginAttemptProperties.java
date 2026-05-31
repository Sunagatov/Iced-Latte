package com.zufar.icedlatte.security.signin.lockout;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "login-attempts")
public record LoginAttemptProperties(Integer maxAttempts, Integer lockoutDurationMinutes) {

    public LoginAttemptProperties {
        if (maxAttempts == null) {
            throw new IllegalArgumentException("login-attempts.max-attempts must not be null");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("login-attempts.max-attempts must be at least 1");
        }
        if (lockoutDurationMinutes == null) {
            throw new IllegalArgumentException("login-attempts.lockout-duration-minutes must not be null");
        }
        if (lockoutDurationMinutes < 1) {
            throw new IllegalArgumentException("login-attempts.lockout-duration-minutes must be at least 1");
        }
    }
}
