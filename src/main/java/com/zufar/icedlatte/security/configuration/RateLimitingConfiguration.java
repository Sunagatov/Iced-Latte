package com.zufar.icedlatte.security.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class RateLimitingConfiguration {

    @Bean
    public InMemoryRateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    public static class InMemoryRateLimiter {
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
            private final long refillIntervalNanos;
            private final long nanosPerToken;
            private double tokens;
            private long lastRefillNanos;
            
            public TokenBucket(int capacity, Duration refillPeriod) {
                this.capacity = capacity;
                this.refillIntervalNanos = refillPeriod.toNanos();
                this.nanosPerToken = refillIntervalNanos / capacity;
                this.tokens = capacity;
                this.lastRefillNanos = System.nanoTime();
            }
            
            public synchronized RateLimitResult tryConsume() {
                refill();
                long resetTime = System.currentTimeMillis() + TimeUnit.NANOSECONDS.toMillis(nanosPerToken * (long)(capacity - tokens));
                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return new RateLimitResult(true, capacity, (int) tokens, resetTime);
                }
                return new RateLimitResult(false, capacity, 0, resetTime);
            }
            
            private void refill() {
                long now = System.nanoTime();
                long elapsed = now - lastRefillNanos;
                if (elapsed > 0) {
                    double tokensToAdd = (double) elapsed / nanosPerToken;
                    tokens = Math.min(capacity, tokens + tokensToAdd);
                    lastRefillNanos = now;
                }
            }
        }
    }

    public record RateLimitResult(boolean allowed, int limit, int remaining, long resetTimeMillis) {}
}
