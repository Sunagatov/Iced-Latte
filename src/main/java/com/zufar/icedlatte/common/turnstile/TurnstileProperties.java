package com.zufar.icedlatte.common.turnstile;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "turnstile")
public record TurnstileProperties(
        boolean enabled,
        boolean checkoutEnabled,
        boolean reviewsEnabled,
        boolean avatarEnabled,
        String secretKey,
        Duration connectTimeout,
        Duration readTimeout,
        List<String> expectedHostnames) {

    public TurnstileProperties {
        secretKey = secretKey == null ? "" : secretKey.trim();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(3) : readTimeout;
        expectedHostnames = expectedHostnames == null
                ? List.of()
                : expectedHostnames.stream()
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();
        if (connectTimeout.isZero() || connectTimeout.isNegative()) {
            throw new IllegalStateException("turnstile.connect-timeout must be positive");
        }
        if (readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalStateException("turnstile.read-timeout must be positive");
        }
        if (!enabled && (checkoutEnabled || reviewsEnabled || avatarEnabled)) {
            throw new IllegalStateException(
                    "turnstile.enabled must be true when feature-specific Turnstile protection is enabled");
        }
        if (enabled && secretKey.isBlank()) {
            throw new IllegalStateException("turnstile.secret-key must be configured when turnstile.enabled=true");
        }
    }

    static TurnstileProperties disabled() {
        return new TurnstileProperties(
                false, false, false, false, "", Duration.ofSeconds(2), Duration.ofSeconds(3), List.of());
    }

    static TurnstileProperties enabledForTests() {
        return new TurnstileProperties(
                true, false, false, false, "test-secret", Duration.ofSeconds(2), Duration.ofSeconds(3), List.of());
    }
}
