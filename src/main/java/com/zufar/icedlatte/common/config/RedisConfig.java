package com.zufar.icedlatte.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

    private final CacheProperties cacheProperties;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("cache.mode: Redis");
        ObjectMapper typedMapper = new ObjectMapper();
        typedMapper.registerModule(new JavaTimeModule());
        typedMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        GenericJackson2JsonRedisSerializer typedSerializer = new GenericJackson2JsonRedisSerializer(typedMapper);

        ObjectMapper plainMapper = new ObjectMapper();
        plainMapper.registerModule(new JavaTimeModule());
        GenericJackson2JsonRedisSerializer plainSerializer = new GenericJackson2JsonRedisSerializer(plainMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheProperties.getDefaultTtl())
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(typedSerializer));

        RedisCacheConfiguration plainConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(plainSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("productById", defaultConfig.entryTtl(cacheProperties.getProductTtl()))
                .withCacheConfiguration("brands", plainConfig.entryTtl(cacheProperties.getBrandsTtl()))
                .withCacheConfiguration("sellers", plainConfig.entryTtl(cacheProperties.getSellersTtl()))
                .withCacheConfiguration("productImageUrl", defaultConfig.entryTtl(cacheProperties.getImageUrlTtl()))
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
