package com.zufar.icedlatte.security.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Configuration
public class RateLimitingConfiguration {

    @Bean
    public InMemoryRateLimiter inMemoryRateLimiter() {
        return new InMemoryRateLimiter();
    }

    public static class InMemoryRateLimiter {
        private final ConcurrentHashMap<String, SlidingWindow> windows = new ConcurrentHashMap<>();

        @SuppressWarnings("unused") // injected as a Spring bean and called via RateLimitingFilter
        public boolean isAllowed(String key, int maxRequests, Duration windowSize) {
            return windows.computeIfAbsent(key, k -> new SlidingWindow(maxRequests, windowSize)).isAllowed();
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
