package com.zufar.icedlatte.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig implements CachingConfigurer {

    private final CacheProperties cacheProperties;

    @Value("${spring.application.version:1}")
    private String appVersion;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("cache.mode: Redis");

        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        TypeFactory tf = mapper.getTypeFactory();

        JavaType listOfString = tf.constructCollectionType(List.class, String.class);

        var productSerializer    = new JacksonJsonRedisSerializer<>(mapper, ProductInfoDto.class);
        var stringSerializer     = new JacksonJsonRedisSerializer<>(mapper, String.class);
        var listStringSerializer = new JacksonJsonRedisSerializer<>(mapper, listOfString);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("v" + appVersion + ":")
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(cacheProperties.getDefaultTtl())
                        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer)))
                .withCacheConfiguration("productById",
                        base.entryTtl(cacheProperties.getProductTtl())
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(productSerializer)))
                .withCacheConfiguration("productImageUrl",
                        base.entryTtl(cacheProperties.getImageUrlTtl())
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer)))
                .withCacheConfiguration("productImageUrls",
                        base.entryTtl(cacheProperties.getImageUrlsTtl())
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(listStringSerializer)))
                .withCacheConfiguration("brands",
                        base.entryTtl(cacheProperties.getBrandsTtl())
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(listStringSerializer)))
                .withCacheConfiguration("sellers",
                        base.entryTtl(cacheProperties.getSellersTtl())
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(listStringSerializer)))
                .build();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException e,
                                            @NonNull Cache cache,
                                            @NonNull Object key) {
                log.warn("cache.get.error: cache={}, key={}, cause={}",
                        cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCachePutError(@NonNull RuntimeException e,
                                            @NonNull Cache cache,
                                            @NonNull Object key,
                                            Object value) {
                log.warn("cache.put.error: cache={}, key={}, cause={}",
                        cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheEvictError(@NonNull RuntimeException e,
                                              @NonNull Cache cache,
                                              @NonNull Object key) {
                log.warn("cache.evict.error: cache={}, key={}, cause={}",
                        cache.getName(), key, e.getMessage());
            }
            @Override
            public void handleCacheClearError(@NonNull RuntimeException e,
                                              @NonNull Cache cache) {
                log.warn("cache.clear.error: cache={}, cause={}",
                        cache.getName(), e.getMessage());
            }
        };
    }
}
