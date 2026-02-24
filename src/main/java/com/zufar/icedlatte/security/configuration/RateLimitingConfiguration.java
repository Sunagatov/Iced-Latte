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
        return new CaffeineRateLimiter();
    }

    static class CaffeineRateLimiter implements RateLimiter {
        private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();

        public RateLimitResult tryConsume(String key, int maxTokens, Duration refillPeriod) {
            try {
                return buckets.get(key, k -> new TokenBucket(maxTokens, refillPeriod)).tryConsume();
            } catch (Exception e) {
                log.error("rate_limit.cache_error: key={}, message={}", key, e.getMessage(), e);
                return new RateLimitResult(true, maxTokens, maxTokens, System.currentTimeMillis() + refillPeriod.toMillis());
            }
        }

        private static class TokenBucket {
            private final int capacity;
            private final long nanosPerToken;
            private double tokens;
            private long lastRefillNanos;

            TokenBucket(int capacity, Duration refillPeriod) {
                this.capacity = capacity;
                this.nanosPerToken = refillPeriod.toNanos() / capacity;
                this.tokens = capacity;
                this.lastRefillNanos = System.nanoTime();
            }

            synchronized RateLimitResult tryConsume() {
                long now = System.nanoTime();
                tokens = Math.min(capacity, tokens + (double)(now - lastRefillNanos) / nanosPerToken);
                lastRefillNanos = now;
                long resetTime = System.currentTimeMillis() + TimeUnit.NANOSECONDS.toMillis(nanosPerToken * (long)(capacity - tokens));
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return new RateLimitResult(true, capacity, (int) tokens, resetTime);
                }
                return new RateLimitResult(false, capacity, 0, resetTime);
            }
        }
    }

    public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {}
}
