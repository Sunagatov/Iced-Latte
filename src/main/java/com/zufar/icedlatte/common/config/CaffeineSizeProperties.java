package com.zufar.icedlatte.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "cache.caffeine")
public record CaffeineSizeProperties(
        @DefaultValue("1000") int rateLimitWarnSize,
        @DefaultValue("5000") int rateLimitFilterSize,
        @DefaultValue("10000") int rateLimitWindowSize,
        @DefaultValue("1000") int redisErrorLogSize,
        @DefaultValue("10000") int temporaryStoreSize
) {}
