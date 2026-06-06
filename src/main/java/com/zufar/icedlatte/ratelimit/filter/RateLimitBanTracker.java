package com.zufar.icedlatte.ratelimit.filter;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties;

class RateLimitBanTracker {

    private final RateLimitProperties properties;
    private final Cache<String, AtomicInteger> blockCounts;

    RateLimitBanTracker(RateLimitProperties properties, CaffeineSizeProperties caffeineSizeProperties) {
        this.properties = properties;
        this.blockCounts = Caffeine.newBuilder()
                .maximumSize(caffeineSizeProperties.rateLimitFilterSize())
                .expireAfterWrite(properties.getBanDuration())
                .build();
    }

    boolean isBanned(String ip) {
        AtomicInteger count = blockCounts.getIfPresent(ip);
        return count != null && count.get() >= properties.getBanThreshold();
    }

    void recordBlock(String ip) {
        blockCounts.get(ip, _ -> new AtomicInteger(0)).incrementAndGet();
    }
}
