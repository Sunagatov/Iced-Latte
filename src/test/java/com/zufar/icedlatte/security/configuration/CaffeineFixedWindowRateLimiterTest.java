package com.zufar.icedlatte.security.configuration;

import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.CaffeineFixedWindowRateLimiter;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.FailPolicy;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.RateLimitResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CaffeineFixedWindowRateLimiter Tests")
class CaffeineFixedWindowRateLimiterTest {

    private final Duration window = Duration.ofMinutes(1);

    @Test
    @DisplayName("requests within limit are allowed")
    void withinLimit_allowed() {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryConsume("key1", 3, window).allowed()).isTrue();
        }
    }

    @Test
    @DisplayName("request exceeding limit is blocked")
    void exceedingLimit_blocked() {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
        for (int i = 0; i < 3; i++) limiter.tryConsume("key2", 3, window);
        assertThat(limiter.tryConsume("key2", 3, window).allowed()).isFalse();
    }

    @Test
    @DisplayName("remaining decrements correctly")
    void remaining_decrementsCorrectly() {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
        RateLimitResult first = limiter.tryConsume("key3", 5, window);
        assertThat(first.remaining()).isEqualTo(4);
        RateLimitResult second = limiter.tryConsume("key3", 5, window);
        assertThat(second.remaining()).isEqualTo(3);
    }

    @Test
    @DisplayName("remaining is 0 when blocked, never negative")
    void remaining_neverNegative_whenBlocked() {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
        for (int i = 0; i < 5; i++) limiter.tryConsume("key4", 2, window);
        RateLimitResult result = limiter.tryConsume("key4", 2, window);
        assertThat(result.allowed()).isFalse();
        assertThat(result.remaining()).isEqualTo(0);
    }

    @Test
    @DisplayName("different keys have independent counters")
    void differentKeys_independentCounters() {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
        for (int i = 0; i < 2; i++) limiter.tryConsume("keyA", 2, window);
        assertThat(limiter.tryConsume("keyA", 2, window).allowed()).isFalse();
        assertThat(limiter.tryConsume("keyB", 2, window).allowed()).isTrue();
    }

    @Test
    @DisplayName("window resets after expiry — counter restarts")
    void windowReset_afterExpiry() throws InterruptedException {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
        Duration shortWindow = Duration.ofMillis(50);
        for (int i = 0; i < 2; i++) limiter.tryConsume("key5", 2, shortWindow);
        assertThat(limiter.tryConsume("key5", 2, shortWindow).allowed()).isFalse();

        Thread.sleep(60);

        assertThat(limiter.tryConsume("key5", 2, shortWindow).allowed()).isTrue();
    }

    @Test
    @DisplayName("fail-closed policy: normal operation still works")
    void failClosed_normalOperationWorks() {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.CLOSED);
        assertThat(limiter.tryConsume("key6", 10, window).allowed()).isTrue();
    }

    @Test
    @DisplayName("resetTimeMillis is in the future")
    void resetTimeMillis_isInFuture() {
        var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
        long before = System.currentTimeMillis();
        RateLimitResult result = limiter.tryConsume("key7", 5, window);
        assertThat(result.resetTimeMillis()).isGreaterThan(before);
    }
}
