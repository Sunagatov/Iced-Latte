package com.zufar.icedlatte.common.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig implements CachingConfigurer {

    private static final com.github.benmanes.caffeine.cache.Cache<String, Boolean> LOGGED_CACHE_ERRORS =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .maximumSize(1_000)
                    .build();

    private final CacheProperties cacheProperties;
    private final List<CacheConfigurationProvider> cacheConfigurationProviders;

    @Value("${spring.application.version:1}")
    private String appVersion;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("cache.mode: Redis");

        var mapper = new tools.jackson.databind.ObjectMapper();
        var stringSerializer = new JacksonJsonRedisSerializer<>(mapper, String.class);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("v" + appVersion + ":")
                .disableCachingNullValues();

        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(cacheProperties.getDefaultTtl())
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer)));

        cacheConfigurationProviders.stream()
                .map(provider -> provider.redisCacheConfigurations(base))
                .map(Map::entrySet)
                .flatMap(Set::stream)
                .forEach(entry -> builder.withCacheConfiguration(entry.getKey(), entry.getValue()));

        return builder.build();
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
            public void handleCacheGetError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
                logCacheError("get", cache.getName(), key, e);
            }

            @Override
            public void handleCachePutError(
                    @NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key, Object value) {
                logCacheError("put", cache.getName(), key, e);
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException e, @NonNull Cache cache, @NonNull Object key) {
                logCacheError("evict", cache.getName(), key, e);
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException e, @NonNull Cache cache) {
                logCacheError("clear", cache.getName(), null, e);
            }
        };
    }

    private static void logCacheError(String operation, String cacheName, Object key, RuntimeException exception) {
        String exceptionClass = exception.getClass().getSimpleName();
        String dedupKey = operation + "|" + cacheName + "|" + exceptionClass;
        if (LOGGED_CACHE_ERRORS.asMap().putIfAbsent(dedupKey, Boolean.TRUE) == null) {
            log.warn("cache.{}.error: cache={}, key={}, exceptionClass={}", operation, cacheName, key, exceptionClass);
        } else {
            log.debug("cache.{}.error: cache={}, key={}, exceptionClass={}", operation, cacheName, key, exceptionClass);
        }
    }
}
