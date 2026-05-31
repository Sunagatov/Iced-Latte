package com.zufar.icedlatte.security.signin.lockout;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "login-attempts")
public record LoginAttemptProperties(int maxAttempts, int lockoutDurationMinutes) {

    public LoginAttemptProperties {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("login-attempts.max-attempts must be at least 1");
        }
        if (lockoutDurationMinutes < 1) {
            throw new IllegalArgumentException("login-attempts.lockout-duration-minutes must be at least 1");
        }
    }
}
