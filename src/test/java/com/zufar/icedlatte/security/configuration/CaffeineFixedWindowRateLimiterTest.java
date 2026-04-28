package com.zufar.icedlatte.security.configuration;

import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.CaffeineFixedWindowRateLimiter;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.FailPolicy;
import com.zufar.icedlatte.security.configuration.RateLimitingConfiguration.RateLimitResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CaffeineFixedWindowRateLimiter unit tests")
class CaffeineFixedWindowRateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Nested
    @DisplayName("tryConsume")
    class TryConsume {

        @Test
        @DisplayName("allows requests up to the configured limit and then blocks")
        void allowsUpToLimitThenBlocks() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);

            RateLimitResult first = limiter.tryConsume("checkout", 3, WINDOW);
            RateLimitResult second = limiter.tryConsume("checkout", 3, WINDOW);
            RateLimitResult third = limiter.tryConsume("checkout", 3, WINDOW);
            RateLimitResult fourth = limiter.tryConsume("checkout", 3, WINDOW);

            assertThat(first.allowed()).isTrue();
            assertThat(first.remaining()).isEqualTo(2);
            assertThat(second.allowed()).isTrue();
            assertThat(second.remaining()).isEqualTo(1);
            assertThat(third.allowed()).isTrue();
            assertThat(third.remaining()).isZero();
            assertThat(fourth.allowed()).isFalse();
            assertThat(fourth.remaining()).isZero();
            assertThat(fourth.limit()).isEqualTo(3);
        }

        @Test
        @DisplayName("keeps counters isolated per key")
        void keepsCountersPerKey() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);

            limiter.tryConsume("cart:user:alice", 1, WINDOW);
            RateLimitResult blockedAlice = limiter.tryConsume("cart:user:alice", 1, WINDOW);
            RateLimitResult allowedBob = limiter.tryConsume("cart:user:bob", 1, WINDOW);

            assertThat(blockedAlice.allowed()).isFalse();
            assertThat(allowedBob.allowed()).isTrue();
            assertThat(allowedBob.remaining()).isZero();
        }

        @Test
        @DisplayName("resets the window after expiry")
        void resetsWindowAfterExpiry() throws InterruptedException {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
            Duration shortWindow = Duration.ofMillis(50);

            limiter.tryConsume("search", 2, shortWindow);
            limiter.tryConsume("search", 2, shortWindow);
            RateLimitResult blocked = limiter.tryConsume("search", 2, shortWindow);

            Thread.sleep(70);

            RateLimitResult afterReset = limiter.tryConsume("search", 2, shortWindow);

            assertThat(blocked.allowed()).isFalse();
            assertThat(afterReset.allowed()).isTrue();
            assertThat(afterReset.remaining()).isEqualTo(1);
        }

        @Test
        @DisplayName("keeps reset time inside the active window")
        void keepsResetTimeInsideActiveWindow() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
            long before = System.currentTimeMillis();

            RateLimitResult result = limiter.tryConsume("products", 5, WINDOW);

            assertThat(result.resetTimeMillis()).isGreaterThan(before);
            assertThat(result.resetTimeMillis()).isLessThanOrEqualTo(before + WINDOW.toMillis() + 50);
        }

        @Test
        @DisplayName("closed policy behaves normally when the cache is healthy")
        void closedPolicyStillAllowsHealthyRequests() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.CLOSED);

            RateLimitResult result = limiter.tryConsume("auth", 10, WINDOW);

            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(9);
        }
    }
}
