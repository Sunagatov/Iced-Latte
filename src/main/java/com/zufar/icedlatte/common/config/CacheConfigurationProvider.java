package com.zufar.icedlatte.common.config;

import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

public interface CacheConfigurationProvider {

    Set<String> cacheNames();

    Map<String, RedisCacheConfiguration> redisCacheConfigurations(RedisCacheConfiguration baseConfiguration);
}
