package com.zufar.icedlatte.common.turnstile;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "turnstile")
public record TurnstileProperties(
        boolean enabled,
        boolean checkoutEnabled,
        boolean reviewsEnabled,
        boolean avatarEnabled,
        String secretKey,
        Duration connectTimeout,
        Duration readTimeout) {

    public TurnstileProperties {
        secretKey = secretKey == null ? "" : secretKey;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(3) : readTimeout;
        if (!enabled && (checkoutEnabled || reviewsEnabled || avatarEnabled)) {
            throw new IllegalStateException(
                    "turnstile.enabled must be true when feature-specific Turnstile protection is enabled");
        }
        if (enabled && secretKey.isBlank()) {
            throw new IllegalStateException("turnstile.secret-key must be configured when turnstile.enabled=true");
        }
    }

    static TurnstileProperties disabled() {
        return new TurnstileProperties(false, false, false, false, "", Duration.ofSeconds(2), Duration.ofSeconds(3));
    }

    static TurnstileProperties enabledForTests() {
        return new TurnstileProperties(
                true, false, false, false, "test-secret", Duration.ofSeconds(2), Duration.ofSeconds(3));
    }
}
