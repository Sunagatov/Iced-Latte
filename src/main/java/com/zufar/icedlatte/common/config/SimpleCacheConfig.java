package com.zufar.icedlatte.common.config;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SimpleCacheConfig {

    private final List<CacheConfigurationProvider> cacheConfigurationProviders;

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        String[] cacheNames = cacheConfigurationProviders.stream()
                .flatMap(provider -> provider.cacheNames().stream())
                .distinct()
                .toArray(String[]::new);
        return new ConcurrentMapCacheManager(cacheNames);
    }
}
