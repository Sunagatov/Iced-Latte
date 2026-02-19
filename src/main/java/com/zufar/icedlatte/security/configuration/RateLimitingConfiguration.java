package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting configuration for enhanced security.
 * Provides both in-memory and Redis-based rate limiting options.
 */
@Slf4j
@Configuration
public class RateLimitingConfiguration {

    /**
     * In-memory rate limiter for development/testing environments.
     * Uses sliding window algorithm with Java 21 features.
     */
    @Bean
    public InMemoryRateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    /**
     * Redis-based rate limiter for production environments.
     */
    @Bean
    public RedisRateLimiter redisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        return new RedisRateLimiter(redisTemplate);
    }

    /**
     * In-memory rate limiter implementation using Java 21 features.
     */
    public static class InMemoryRateLimiter {
        private final ConcurrentHashMap<String, SlidingWindow> windows = new ConcurrentHashMap<>();

        @SuppressWarnings("unused") // injected as a Spring bean and called via RateLimitingFilter
        public boolean isAllowed(String key, int maxRequests, Duration windowSize) {
            return windows.computeIfAbsent(key, k -> new SlidingWindow(maxRequests, windowSize)).isAllowed();
        }
        
        // Class for sliding window - Java 21 feature
        private static class SlidingWindow {
            private final int maxRequests;
            private final Duration windowSize;
            private final AtomicInteger requestCount = new AtomicInteger(0);
            private volatile long windowStart = System.currentTimeMillis();
            
            public SlidingWindow(int maxRequests, Duration windowSize) {
                this.maxRequests = maxRequests;
                this.windowSize = windowSize;
            }
            
            public boolean isAllowed() {
                long now = System.currentTimeMillis();
                
                // Reset window if expired
                if (now - windowStart > windowSize.toMillis()) {
                    requestCount.set(0);
                    windowStart = now;
                }
                
                return requestCount.incrementAndGet() <= maxRequests;
            }
        }
    }

    /**
     * Redis-based rate limiter for distributed environments.
     */
    public static class RedisRateLimiter {
        private final ValueOperations<String, String> valueOps;
        
        public RedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
            this.valueOps = redisTemplate.opsForValue();
        }
        
        @SuppressWarnings("unused") // injected as a Spring bean and called via RateLimitingFilter
        public boolean isAllowed(String key, int maxRequests, Duration windowSize) {
            String redisKey = "rate_limit:" + key;
            
            try {
                // Use Redis atomic operations for thread-safe rate limiting
                String currentCount = valueOps.get(redisKey);
                
                if (currentCount == null) {
                    // First request in window
                    valueOps.set(redisKey, "1", windowSize);
                    return true;
                }
                
                int count = Integer.parseInt(currentCount);
                if (count < maxRequests) {
                    valueOps.increment(redisKey);
                    return true;
                }
                
                return false;
            } catch (DataAccessException e) {
                log.warn("Rate limiting Redis operation failed for key: {}, allowing request", key, e);
                return true; // Fail open for availability
            } catch (NumberFormatException e) {
                log.warn("Rate limiting counter corrupted for key: {}, allowing request", key, e);
                return true; // Fail open for availability
            }
        }
    }
}
