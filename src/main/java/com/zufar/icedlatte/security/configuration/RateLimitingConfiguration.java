package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

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
            SlidingWindow window = windows.getIfPresent(key);
            if (window == null) {
                window = new SlidingWindow(maxRequests, windowSize);
                windows.put(key, window);
            }
            return window.isAllowed();
        }
        
        private static class SlidingWindow {
            private final int maxRequests;
            private final Duration windowSize;
            private final AtomicInteger requestCount = new AtomicInteger(0);
            private volatile long windowStart = System.currentTimeMillis();
            
            public SlidingWindow(int maxRequests, Duration windowSize) {
                this.maxRequests = maxRequests;
                this.windowSize = windowSize;
            }
            
            public synchronized boolean isAllowed() {
                long now = System.currentTimeMillis();
                if (now - windowStart > windowSize.toMillis()) {
                    requestCount.set(0);
                    windowStart = now;
                }
                return requestCount.incrementAndGet() <= maxRequests;
            }
        }
    }

}
