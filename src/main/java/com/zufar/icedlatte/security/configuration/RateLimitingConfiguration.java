package com.zufar.icedlatte.security.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
public class RateLimitingConfiguration {

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            local pttl = redis.call('PTTL', KEYS[1])
            return {current, pttl}
            """, List.class);

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RateLimiter redisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        log.info("rate_limit.mode: Redis");
        return (key, maxTokens, windowDuration) -> {
            try {
                @SuppressWarnings("rawtypes")
                List result = redisTemplate.execute(
                        RATE_LIMIT_SCRIPT,
                        List.of("rate:" + key),
                        String.valueOf(windowDuration.toMillis())
                );
                long count = ((Number) result.get(0)).longValue();
                long pttl = ((Number) result.get(1)).longValue();
                long resetTimeMillis = System.currentTimeMillis() + Math.max(0, pttl);
                int remaining = (int) Math.max(0, maxTokens - count);
                return new RateLimitResult(count <= maxTokens, maxTokens, remaining, resetTimeMillis);
            } catch (Exception e) {
                log.error("rate_limit.redis_error: key={}, message={}", key, e.getMessage(), e);
                return new RateLimitResult(true, maxTokens, maxTokens, System.currentTimeMillis() + windowDuration.toMillis());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter caffeineRateLimiter() {
        log.info("rate_limit.mode: in-memory Caffeine (Redis not configured)");
        return new CaffeineFixedWindowRateLimiter();
    }

    // Fixed-window counter — same algorithm as the Redis Lua script so local and prod behave identically.
    // On any backend error the limiter returns allowed=true (fail-open) to avoid blocking legitimate
    // traffic during Redis/cache incidents. NOTE: both PreAuthRateLimitingFilter and RateLimitingFilter
    // share this same backend bean, so a Redis outage disables both layers simultaneously.
    static class CaffeineFixedWindowRateLimiter implements RateLimiter {
        private final Cache<String, FixedWindow> windows = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();

        public RateLimitResult tryConsume(String key, int maxTokens, Duration windowDuration) {
            try {
                FixedWindow window = windows.get(key, _ -> new FixedWindow(windowDuration.toMillis()));
                return window.tryConsume(maxTokens, windowDuration.toMillis());
            } catch (Exception e) {
                log.error("rate_limit.cache_error: key={}, message={}", key, e.getMessage(), e);
                return new RateLimitResult(true, maxTokens, maxTokens, System.currentTimeMillis() + windowDuration.toMillis());
            }
        }

        private static class FixedWindow {
            private final AtomicLong count = new AtomicLong(0);
            private volatile long windowStartMillis;
            private final long windowMillis;

            FixedWindow(long windowMillis) {
                this.windowMillis = windowMillis;
                this.windowStartMillis = System.currentTimeMillis();
            }

            synchronized RateLimitResult tryConsume(int maxTokens, long windowDurationMillis) {
                long now = System.currentTimeMillis();
                if (now - windowStartMillis >= windowMillis) {
                    count.set(0);
                    windowStartMillis = now;
                }
                long resetTimeMillis = windowStartMillis + windowDurationMillis;
                long current = count.incrementAndGet();
                int remaining = (int) Math.max(0, maxTokens - current);
                return new RateLimitResult(current <= maxTokens, maxTokens, remaining, resetTimeMillis);
            }
        }
    }

    public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {}
}
