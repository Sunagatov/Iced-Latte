package com.zufar.icedlatte.ratelimit.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.ratelimit.api.RateLimitResult;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitingConfiguration.CaffeineFixedWindowRateLimiter;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitingConfiguration.FailPolicy;

@DisplayName("CaffeineFixedWindowRateLimiter unit tests")
class CaffeineFixedWindowRateLimiterTest {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Nested
    @DisplayName("tryConsume")
    class TryConsume {

        @Test
        @DisplayName("allows requests up to the configured limit and then blocks")
        void allowsUpToLimitThenBlocks() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);

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
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);

            limiter.tryConsume("cart:user:alice", 1, WINDOW);
            RateLimitResult blockedAlice = limiter.tryConsume("cart:user:alice", 1, WINDOW);
            RateLimitResult allowedBob = limiter.tryConsume("cart:user:bob", 1, WINDOW);

            assertThat(blockedAlice.allowed()).isFalse();
            assertThat(allowedBob.allowed()).isTrue();
            assertThat(allowedBob.remaining()).isZero();
        }

        @Test
        @DisplayName("resets the window after expiry")
        void resetsWindowAfterExpiry() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);
            Duration shortWindow = Duration.ofMillis(50);

            limiter.tryConsume("search", 2, shortWindow);
            limiter.tryConsume("search", 2, shortWindow);
            RateLimitResult blocked = limiter.tryConsume("search", 2, shortWindow);

            RateLimitResult afterReset = awaitWindowReset(limiter, shortWindow);

            assertThat(blocked.allowed()).isFalse();
            assertThat(afterReset.allowed()).isTrue();
            assertThat(afterReset.remaining()).isEqualTo(1);
        }

        @Test
        @DisplayName("starts a fresh fixed window when a key is reused with a different duration")
        void resetsWindowWhenDurationChanges() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);

            limiter.tryConsume("shared-key", 1, Duration.ofMinutes(1));
            RateLimitResult blocked = limiter.tryConsume("shared-key", 1, Duration.ofMinutes(1));
            RateLimitResult durationChanged = limiter.tryConsume("shared-key", 1, Duration.ofSeconds(5));

            assertThat(blocked.allowed()).isFalse();
            assertThat(durationChanged.allowed()).isTrue();
            assertThat(durationChanged.remaining()).isZero();
            assertThat(durationChanged.windowSeconds()).isEqualTo(5);
        }

        @Test
        void alternatingWindowDurationsPreservesBothCounters() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);
            limiter.tryConsume("shared-key", 1, Duration.ofMinutes(1));
            limiter.tryConsume("shared-key", 1, Duration.ofSeconds(5));

            assertThat(limiter.tryConsume("shared-key", 1, Duration.ofMinutes(1))
                            .allowed())
                    .isFalse();
            assertThat(limiter.tryConsume("shared-key", 1, Duration.ofSeconds(5))
                            .allowed())
                    .isFalse();
        }

        @Test
        void longWindowsDoNotResetAfterTenMinutesOfInactivity() {
            AtomicLong nanos = new AtomicLong();
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000, nanos::get);
            limiter.tryConsume("daily-budget", 1, Duration.ofHours(24));
            nanos.addAndGet(Duration.ofMinutes(11).toNanos());

            assertThat(limiter.tryConsume("daily-budget", 1, Duration.ofHours(24))
                            .allowed())
                    .isFalse();
        }

        @Test
        void fractionalWindowSecondsAreRoundedUp() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);
            assertThat(limiter.tryConsume("fractional", 1, Duration.ofMillis(1500))
                            .windowSeconds())
                    .isEqualTo(2);
        }

        @Test
        void readingABlockedWindowDoesNotExtendItsExpiry() {
            AtomicLong nanos = new AtomicLong();
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000, nanos::get);
            assertThat(limiter.tryConsume("key", 1, WINDOW).allowed()).isTrue();
            nanos.addAndGet(Duration.ofSeconds(30).toNanos());
            assertThat(limiter.tryConsume("key", 1, WINDOW).allowed()).isFalse();
            nanos.addAndGet(Duration.ofSeconds(30).toNanos());
            assertThat(limiter.tryConsume("key", 1, WINDOW).allowed()).isTrue();
        }

        @Test
        void concurrentRequestsCannotExceedTheBudget() throws Exception {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);
            Callable<Boolean> request =
                    () -> limiter.tryConsume("shared", 10, WINDOW).allowed();
            try (var executor = Executors.newFixedThreadPool(8)) {
                var results = executor.invokeAll(Collections.nCopies(100, request));
                int allowed = 0;
                for (var result : results) {
                    if (result.get()) {
                        allowed++;
                    }
                }
                assertThat(allowed).isEqualTo(10);
            }
        }

        @Test
        @DisplayName("keeps reset time inside the active window")
        void keepsResetTimeInsideActiveWindow() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, 10_000);
            long before = System.currentTimeMillis();

            RateLimitResult result = limiter.tryConsume("products", 5, WINDOW);

            assertThat(result.resetTimeMillis()).isGreaterThan(before);
            assertThat(result.resetTimeMillis()).isLessThanOrEqualTo(before + WINDOW.toMillis() + 50);
        }

        @Test
        @DisplayName("closed policy behaves normally when the cache is healthy")
        void closedPolicyStillAllowsHealthyRequests() {
            var limiter = new CaffeineFixedWindowRateLimiter(FailPolicy.CLOSED, 10_000);

            RateLimitResult result = limiter.tryConsume("auth", 10, WINDOW);

            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(9);
        }
    }

    private static RateLimitResult awaitWindowReset(CaffeineFixedWindowRateLimiter limiter, Duration window) {
        long deadline = System.nanoTime() + Duration.ofSeconds(1).toNanos();
        RateLimitResult result;
        do {
            result = limiter.tryConsume("search", 2, window);
            if (result.allowed()) {
                return result;
            }
            LockSupport.parkNanos(Duration.ofMillis(1).toNanos());
        } while (System.nanoTime() < deadline);
        return result;
    }
}
