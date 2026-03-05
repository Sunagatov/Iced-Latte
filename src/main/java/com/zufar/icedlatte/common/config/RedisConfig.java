package com.zufar.icedlatte.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

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

        tools.jackson.databind.ObjectMapper typedMapper = new tools.jackson.databind.ObjectMapper().rebuild()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                        DefaultTyping.OBJECT_AND_NON_CONCRETE)
                .build();
        GenericJacksonJsonRedisSerializer typedSerializer = new GenericJacksonJsonRedisSerializer(typedMapper);
        GenericJacksonJsonRedisSerializer plainSerializer = new GenericJacksonJsonRedisSerializer(new tools.jackson.databind.ObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getDefaultTtl())
                .prefixCacheNameWith("v" + appVersion + ":")
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(typedSerializer));

        RedisCacheConfiguration plainConfig = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith("v" + appVersion + ":")
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(plainSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("productById", defaultConfig.entryTtl(cacheProperties.getProductTtl()))
                .withCacheConfiguration("brands", plainConfig.entryTtl(cacheProperties.getBrandsTtl()))
                .withCacheConfiguration("sellers", plainConfig.entryTtl(cacheProperties.getSellersTtl()))
                .withCacheConfiguration("productImageUrl", defaultConfig.entryTtl(cacheProperties.getImageUrlTtl()))
                .withCacheConfiguration("productImageUrls", plainConfig.entryTtl(cacheProperties.getImageUrlsTtl()))
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
}
