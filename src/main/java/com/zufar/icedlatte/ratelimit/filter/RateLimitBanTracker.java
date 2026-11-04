package com.zufar.icedlatte.ratelimit.filter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.zufar.icedlatte.common.config.CaffeineSizeProperties;
import com.zufar.icedlatte.ratelimit.configuration.RateLimitProperties;

class RateLimitBanTracker {

    private final RateLimitProperties properties;
    private final Cache<String, AtomicInteger> blockCounts;
    private final Cache<String, Long> banStarts;
    private final Ticker ticker;

    RateLimitBanTracker(RateLimitProperties properties, CaffeineSizeProperties caffeineSizeProperties) {
        this(properties, caffeineSizeProperties, Ticker.systemTicker());
    }

    RateLimitBanTracker(RateLimitProperties properties, CaffeineSizeProperties caffeineSizeProperties, Ticker ticker) {
        this.properties = properties;
        this.ticker = ticker;
        this.blockCounts = Caffeine.newBuilder()
                .ticker(ticker)
                .maximumSize(caffeineSizeProperties.rateLimitFilterSize())
                .expireAfterWrite(properties.getBanDuration())
                .build();
        this.banStarts = Caffeine.newBuilder()
                .ticker(ticker)
                .maximumSize(caffeineSizeProperties.rateLimitFilterSize())
                .expireAfterWrite(properties.getBanDuration())
                .build();
    }

    boolean isBanned(String ip) {
        return !remainingBanDuration(ip).isZero();
    }

    Duration remainingBanDuration(String ip) {
        Long started = banStarts.getIfPresent(ip);
        if (started == null) {
            return Duration.ZERO;
        }
        long remaining = properties.getBanDuration().toNanos() - (ticker.read() - started);
        return Duration.ofNanos(Math.max(0, remaining));
    }

    void recordBlock(String ip) {
        int blocks = blockCounts.get(ip, _ -> new AtomicInteger(0)).incrementAndGet();
        if (blocks == properties.getBanThreshold()) {
            banStarts.asMap().putIfAbsent(ip, ticker.read());
        }
    }
}
