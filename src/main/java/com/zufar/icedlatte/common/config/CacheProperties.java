package com.zufar.icedlatte.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    private Duration defaultTtl = Duration.ofMinutes(10);
    private Duration productTtl = Duration.ofMinutes(10);
    private Duration brandsTtl = Duration.ofHours(24);
    private Duration sellersTtl = Duration.ofHours(24);
    private Duration imageUrlTtl = Duration.ofMinutes(50);
    private Duration imageUrlsTtl = Duration.ofHours(24);
}
