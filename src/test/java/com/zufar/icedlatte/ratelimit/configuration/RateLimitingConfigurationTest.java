package com.zufar.icedlatte.ratelimit.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.ratelimit.api.RateLimiter;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingConfiguration unit tests")
class RateLimitingConfigurationTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    private final RateLimitingConfiguration configuration = new RateLimitingConfiguration();

    @Nested
    @DisplayName("Redis-backed limiters")
    class RedisBackedLimiters {

        @Test
        @DisplayName("fail-open limiter allows traffic when Redis execution fails")
        void failOpenLimiterAllowsWhenRedisFails() {
            when(redisTemplate.execute(anyRedisScript(), eq(List.of("rate:1000:search:ip:1.2.3.4")), eq("1000")))
                    .thenThrow(new IllegalStateException("redis down"));

            RateLimiter limiter = configuration.openRedisRateLimiter(redisTemplate);
            var result = limiter.tryConsume("search:ip:1.2.3.4", 5, Duration.ofSeconds(1));

            assertThat(result.allowed()).isTrue();
            assertThat(result.limit()).isEqualTo(5);
            assertThat(result.remaining()).isEqualTo(5);
            assertThat(result.resetTimeMillis()).isGreaterThan(System.currentTimeMillis());
        }

        @Test
        @DisplayName("fail-closed limiter blocks traffic when Redis execution fails")
        void failClosedLimiterBlocksWhenRedisFails() {
            when(redisTemplate.execute(anyRedisScript(), eq(List.of("rate:1000:auth:ip:1.2.3.4")), eq("1000")))
                    .thenThrow(new IllegalStateException("redis down"));

            RateLimiter limiter = configuration.closedRedisRateLimiter(redisTemplate);
            var result = limiter.tryConsume("auth:ip:1.2.3.4", 10, Duration.ofSeconds(1));

            assertThat(result.allowed()).isFalse();
            assertThat(result.limit()).isEqualTo(10);
            assertThat(result.remaining()).isZero();
        }

        @Test
        @DisplayName("maps Redis counter result into the public rate-limit contract")
        void mapsRedisCounterResult() {
            when(redisTemplate.execute(anyRedisScript(), eq(List.of("rate:60000:global:user:alice")), eq("60000")))
                    .thenReturn(List.of(3L, 42_000L));

            RateLimiter limiter = configuration.openRedisRateLimiter(redisTemplate);
            long before = System.currentTimeMillis();
            var result = limiter.tryConsume("global:user:alice", 5, Duration.ofMinutes(1));

            assertThat(result.allowed()).isTrue();
            assertThat(result.limit()).isEqualTo(5);
            assertThat(result.remaining()).isEqualTo(2);
            assertThat(result.resetTimeMillis()).isBetween(before + 41_000L, before + 43_000L);
            verify(redisTemplate).execute(anyRedisScript(), eq(List.of("rate:60000:global:user:alice")), eq("60000"));
        }

        @Test
        @DisplayName("includes the window duration in Redis keys")
        void includesWindowDurationInRedisKey() {
            when(redisTemplate.execute(anyRedisScript(), eq(List.of("rate:5000:global:user:alice")), eq("5000")))
                    .thenReturn(List.of(1L, 5_000L));

            RateLimiter limiter = configuration.openRedisRateLimiter(redisTemplate);
            var result = limiter.tryConsume("global:user:alice", 5, Duration.ofSeconds(5));

            assertThat(result.allowed()).isTrue();
            verify(redisTemplate).execute(anyRedisScript(), eq(List.of("rate:5000:global:user:alice")), eq("5000"));
        }
    }

    @Nested
    @DisplayName("Caffeine-backed limiters")
    class CaffeineBackedLimiters {

        @Test
        @DisplayName("pre-auth flood fallback uses open policy semantics")
        void preAuthFloodFallbackUsesOpenPolicy() {
            RateLimiter limiter = configuration.openCaffeineRateLimiter(
                    new CaffeineSizeProperties(1_000, 5_000, 10_000, 1_000, 10_000));

            var first = limiter.tryConsume("global:ip:1.2.3.4", 1, Duration.ofMinutes(1));
            var second = limiter.tryConsume("global:ip:1.2.3.4", 1, Duration.ofMinutes(1));

            assertThat(first.allowed()).isTrue();
            assertThat(second.allowed()).isFalse();
        }

        @Test
        @DisplayName("pre-auth auth fallback uses closed-policy limiter behavior")
        void preAuthAuthFallbackCreatesClosedPolicyLimiter() {
            RateLimiter limiter = configuration.closedCaffeineRateLimiter(
                    new CaffeineSizeProperties(1_000, 5_000, 10_000, 1_000, 10_000));

            var result = limiter.tryConsume("auth:ip:1.2.3.4", 2, Duration.ofMinutes(1));

            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(1);
        }
    }

    private RedisScript<List<Long>> anyRedisScript() {
        return any();
    }
}
