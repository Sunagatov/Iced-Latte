package com.zufar.icedlatte.email.api.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.email.exception.IncorrectTokenException;
import com.zufar.icedlatte.openapi.dto.UserRegistrationRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisTokenCache implements TokenCache {

    private static final String KEY_PREFIX = "email:token:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    @PostConstruct
    void init() { log.info("token_cache.mode: Redis"); }

    @Override
    public void addToken(String tokenKey, UserRegistrationRequest request) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenKey,
                    objectMapper.writeValueAsString(request),
                    Duration.ofMinutes(expireTimeMinutes));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize registration request", e);
        }
    }

    @Override
    public UserRegistrationRequest getToken(String tokenKey) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + tokenKey);
        if (json == null) throw new IncorrectTokenException();
        try {
            return objectMapper.readValue(json, UserRegistrationRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize registration request", e);
        }
    }

    @Override
    public void removeToken(String tokenKey) {
        redisTemplate.delete(KEY_PREFIX + tokenKey);
    }
}
