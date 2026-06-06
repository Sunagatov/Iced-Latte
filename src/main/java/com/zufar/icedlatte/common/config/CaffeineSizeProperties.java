package com.zufar.icedlatte.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "cache.caffeine")
public record CaffeineSizeProperties(
        @DefaultValue("1000") int rateLimitWarnSize,
        @DefaultValue("5000") int rateLimitFilterSize,
        @DefaultValue("10000") int rateLimitWindowSize,
        @DefaultValue("1000") int redisErrorLogSize,
        @DefaultValue("10000") int temporaryStoreSize) {

    public CaffeineSizeProperties {
        requirePositive(rateLimitWarnSize, "cache.caffeine.rate-limit-warn-size");
        requirePositive(rateLimitFilterSize, "cache.caffeine.rate-limit-filter-size");
        requirePositive(rateLimitWindowSize, "cache.caffeine.rate-limit-window-size");
        requirePositive(redisErrorLogSize, "cache.caffeine.redis-error-log-size");
        requirePositive(temporaryStoreSize, "cache.caffeine.temporary-store-size");
    }

    private static void requirePositive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException(propertyName + " must be positive");
        }
    }
}
