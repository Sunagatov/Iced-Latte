package com.zufar.icedlatte.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final CacheProperties cacheProperties;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getDefaultTtl())
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("productById", defaultConfig.entryTtl(cacheProperties.getProductTtl()))
                .withCacheConfiguration("userById", defaultConfig.entryTtl(cacheProperties.getUserTtl()))
                .withCacheConfiguration("brands", defaultConfig.entryTtl(cacheProperties.getBrandsTtl()))
                .withCacheConfiguration("sellers", defaultConfig.entryTtl(cacheProperties.getSellersTtl()))
                .build();
    }
}
