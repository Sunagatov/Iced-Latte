package com.zufar.icedlatte.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cache.caffeine")
public class CaffeineSizeProperties {

    private int rateLimitWarnSize = 1_000;
    private int rateLimitFilterSize = 5_000;
    private int rateLimitWindowSize = 10_000;
    private int redisErrorLogSize = 1_000;
    private int temporaryStoreSize = 10_000;
}
