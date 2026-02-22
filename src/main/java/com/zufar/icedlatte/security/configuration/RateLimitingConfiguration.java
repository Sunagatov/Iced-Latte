package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;

@Slf4j
@Configuration
public class RateLimitingConfiguration {

    @Bean
    public InMemoryRateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    public static class InMemoryRateLimiter {
        private final Cache<String, SlidingWindow> windows = CacheBuilder.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();

        @SuppressWarnings("unused") // injected as a Spring bean and called via RateLimitingFilter
        public boolean isAllowed(String key, int maxRequests, Duration windowSize) {
            try {
                return windows.get(key, () -> new SlidingWindow(maxRequests, windowSize)).isAllowed();
            } catch (Exception e) {
                log.error("rate_limit.cache_error: key={}", key, e);
                return true;
            }
        }
        
        private static class SlidingWindow {
            private final int maxRequests;
            private final Duration windowSize;
            private int requestCount = 0;
            private long windowStart = System.currentTimeMillis();
            
            public SlidingWindow(int maxRequests, Duration windowSize) {
                this.maxRequests = maxRequests;
                this.windowSize = windowSize;
            }
            
            public synchronized boolean isAllowed() {
                long now = System.currentTimeMillis();
                if (now - windowStart > windowSize.toMillis()) {
                    requestCount = 0;
                    windowStart = now;
                }
                return ++requestCount <= maxRequests;
            }
        }
    }

}
