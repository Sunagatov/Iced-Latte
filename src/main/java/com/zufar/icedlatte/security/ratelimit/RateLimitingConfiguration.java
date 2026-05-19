package com.zufar.icedlatte.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties({RateLimitProperties.class, com.zufar.icedlatte.common.config.CaffeineSizeProperties.class})
public class RateLimitingConfiguration {

    private static final Cache<String, Boolean> LOGGED_LIMITER_ERRORS = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(1_000)
            .build();

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

    @Bean("openRateLimiter")
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RateLimiter openRedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        log.info("rate_limit.mode.open: Redis");
        return redisRateLimiterWithPolicy(redisTemplate, FailPolicy.OPEN);
    }

    @Bean("openRateLimiter")
    @ConditionalOnMissingBean(name = "openRateLimiter")
    public RateLimiter openCaffeineRateLimiter(com.zufar.icedlatte.common.config.CaffeineSizeProperties caffeineSizeProperties) {
        log.info("rate_limit.mode.open: in-memory Caffeine");
        return new CaffeineFixedWindowRateLimiter(FailPolicy.OPEN, caffeineSizeProperties.getRateLimitWindowSize());
    }

    @Bean("closedRateLimiter")
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public RateLimiter closedRedisRateLimiter(RedisTemplate<String, String> redisTemplate) {
        log.info("rate_limit.mode.closed: Redis");
        return redisRateLimiterWithPolicy(redisTemplate, FailPolicy.CLOSED);
    }

    @Bean("closedRateLimiter")
    @ConditionalOnMissingBean(name = "closedRateLimiter")
    public RateLimiter closedCaffeineRateLimiter(com.zufar.icedlatte.common.config.CaffeineSizeProperties caffeineSizeProperties) {
        log.info("rate_limit.mode.closed: in-memory Caffeine");
        return new CaffeineFixedWindowRateLimiter(FailPolicy.CLOSED, caffeineSizeProperties.getRateLimitWindowSize());
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
                long count = ((Number) result.getFirst()).longValue();
                long pttl = ((Number) result.get(1)).longValue();
                long resetTimeMillis = System.currentTimeMillis() + Math.max(0, pttl);
                int remaining = (int) Math.max(0, maxTokens - count);
                return new RateLimitResult(count <= maxTokens, maxTokens, remaining, resetTimeMillis);
            } catch (Exception e) {
                logRepeatedLimiterFailure("rate_limit.redis_error", key, policy, e, policy == FailPolicy.OPEN);
                boolean allowed = policy == FailPolicy.OPEN;
                return new RateLimitResult(allowed, maxTokens, allowed ? maxTokens : 0,
                        System.currentTimeMillis() + windowDuration.toMillis());
            }
        };
    }

    // Fixed-window counter — same algorithm as the Redis Lua script so local and prod behave identically.
    static class CaffeineFixedWindowRateLimiter implements RateLimiter {
        private final FailPolicy failPolicy;
        private final Cache<String, FixedWindow> windows;

        CaffeineFixedWindowRateLimiter(FailPolicy failPolicy, int windowSize) {
            this.failPolicy = failPolicy;
            this.windows = Caffeine.newBuilder()
                    .maximumSize(windowSize)
                    .expireAfterAccess(10, TimeUnit.MINUTES)
                    .build();
        }

        public RateLimitResult tryConsume(String key, int maxTokens, Duration windowDuration) {
            try {
                FixedWindow window = windows.get(key, _ -> new FixedWindow(windowDuration.toMillis()));
                return window.tryConsume(maxTokens);
            } catch (Exception e) {
                logRepeatedLimiterFailure("rate_limit.cache_error", key, failPolicy, e, false);
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

    private static void logRepeatedLimiterFailure(String event,
                                                  String key,
                                                  FailPolicy policy,
                                                  Exception exception,
                                                  boolean warnOnFirst) {
        String exceptionClass = exception.getClass().getSimpleName();
        String dedupKey = event + "|" + policy + "|" + exceptionClass;
        if (LOGGED_LIMITER_ERRORS.asMap().putIfAbsent(dedupKey, Boolean.TRUE) == null) {
            if (warnOnFirst) {
                log.warn("{}: key={}, policy={}, exceptionClass={}", event, key, policy, exceptionClass);
            } else {
                log.error("{}: key={}, policy={}, exceptionClass={}", event, key, policy, exceptionClass, exception);
            }
        } else {
            log.debug("{}: key={}, policy={}, exceptionClass={}", event, key, policy, exceptionClass);
        }
    }

}
