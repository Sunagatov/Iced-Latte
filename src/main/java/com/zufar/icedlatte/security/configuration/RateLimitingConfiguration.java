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

    public enum FailPolicy { OPEN, CLOSED }

    // --- pre-auth flood bean: fail-OPEN — public read traffic should not 429 on transient Redis transport errors ---

    @Bean("preAuthFloodRateLimiter")
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RateLimiter preAuthFloodRedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        log.info("rate_limit.pre-auth.flood.mode: Redis (fail-open)");
        return redisRateLimiterWithPolicy(redisTemplate, FailPolicy.OPEN);
    }

    @Bean("preAuthFloodRateLimiter")
    @ConditionalOnMissingBean(name = "preAuthFloodRateLimiter")
    public RateLimiter preAuthFloodCaffeineRateLimiter() {
        log.info("rate_limit.pre-auth.flood.mode: in-memory Caffeine (fail-open)");
        return new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
    }

    // --- auth pre-auth bean: fail-CLOSED — deny auth floods when the backend limiter is unavailable ---

    @Bean("preAuthAuthRateLimiter")
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RateLimiter preAuthAuthRedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        log.info("rate_limit.pre-auth.auth.mode: Redis (fail-closed)");
        return redisRateLimiterWithPolicy(redisTemplate, FailPolicy.CLOSED);
    }

    @Bean("preAuthAuthRateLimiter")
    @ConditionalOnMissingBean(name = "preAuthAuthRateLimiter")
    public RateLimiter preAuthAuthCaffeineRateLimiter() {
        log.info("rate_limit.pre-auth.auth.mode: in-memory Caffeine (fail-closed)");
        return new CaffeineFixedWindowRateLimiter(FailPolicy.CLOSED);
    }

    // --- post-auth bean: fail-OPEN — allow on backend error to avoid blocking legitimate traffic ---

    @Bean("postAuthRateLimiter")
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RateLimiter postAuthRedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        log.info("rate_limit.post-auth.mode: Redis (fail-open)");
        return redisRateLimiterWithPolicy(redisTemplate, FailPolicy.OPEN);
    }

    @Bean("postAuthRateLimiter")
    @ConditionalOnMissingBean(name = "postAuthRateLimiter")
    public RateLimiter postAuthCaffeineRateLimiter() {
        log.info("rate_limit.post-auth.mode: in-memory Caffeine (fail-open)");
        return new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN);
    }

    private RateLimiter redisRateLimiterWithPolicy(RedisTemplate<String, String> redisTemplate, FailPolicy policy) {
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
                if (policy == FailPolicy.OPEN) {
                    log.warn("rate_limit.redis_error: key={}, policy={}, message={}", key, policy, e.getMessage());
                } else {
                    log.error("rate_limit.redis_error: key={}, policy={}, message={}", key, policy, e.getMessage(), e);
                }
                boolean allowed = policy == FailPolicy.OPEN;
                return new RateLimitResult(allowed, maxTokens, allowed ? maxTokens : 0,
                        System.currentTimeMillis() + windowDuration.toMillis());
            }
        };
    }

    // Fixed-window counter — same algorithm as the Redis Lua script so local and prod behave identically.
    static class CaffeineFixedWindowRateLimiter implements RateLimiter {
        private final FailPolicy failPolicy;
        private final Cache<String, FixedWindow> windows = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();

        CaffeineFixedWindowRateLimiter(FailPolicy failPolicy) {
            this.failPolicy = failPolicy;
        }

        public RateLimitResult tryConsume(String key, int maxTokens, Duration windowDuration) {
            try {
                FixedWindow window = windows.get(key, _ -> new FixedWindow(windowDuration.toMillis()));
                return window.tryConsume(maxTokens);
            } catch (Exception e) {
                log.error("rate_limit.cache_error: key={}, policy={}, message={}", key, failPolicy, e.getMessage(), e);
                boolean allowed = failPolicy == FailPolicy.OPEN;
                return new RateLimitResult(allowed, maxTokens, allowed ? maxTokens : 0,
                        System.currentTimeMillis() + windowDuration.toMillis());
            }
        }

        private static class FixedWindow {
            private long count = 0;
            private long windowStartMillis;
            private final long windowMillis;

            FixedWindow(long windowMillis) {
                this.windowMillis = windowMillis;
                this.windowStartMillis = System.currentTimeMillis();
            }

            synchronized RateLimitResult tryConsume(int maxTokens) {
                long now = System.currentTimeMillis();
                if (now - windowStartMillis >= windowMillis) {
                    count = 0;
                    windowStartMillis = now;
                }
                long resetTimeMillis = windowStartMillis + windowMillis;
                long current = ++count;
                int remaining = (int) Math.max(0, maxTokens - current);
                return new RateLimitResult(current <= maxTokens, maxTokens, remaining, resetTimeMillis);
            }
        }
    }

    public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {}
}
