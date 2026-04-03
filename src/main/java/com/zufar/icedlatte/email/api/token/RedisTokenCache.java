package com.zufar.icedlatte.email.api.token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    private record CacheEntry(UserRegistrationRequest request, TokenPurpose purpose) {
        @JsonCreator
        CacheEntry(@JsonProperty("request") UserRegistrationRequest request,
                   @JsonProperty("purpose") TokenPurpose purpose) {
            this.request = request;
            this.purpose = purpose;
        }
    }

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${temporary-cache.time.token}")
    private int expireTimeMinutes;

    @PostConstruct
    void init() { log.info("token_cache.mode: Redis"); }

    @Override
    public void addToken(String tokenKey, UserRegistrationRequest request, TokenPurpose purpose) {
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + tokenKey,
                    objectMapper.writeValueAsString(new CacheEntry(request, purpose)),
                    Duration.ofMinutes(expireTimeMinutes));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize token cache entry", e);
        }
    }

    @Override
    public UserRegistrationRequest getToken(String tokenKey, TokenPurpose expectedPurpose) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + tokenKey);
        if (json == null) throw new IncorrectTokenException();
        try {
            CacheEntry entry = objectMapper.readValue(json, CacheEntry.class);
            if (entry.purpose() != expectedPurpose) throw new IncorrectTokenException();
            return entry.request();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize token cache entry", e);
        }
    }

    @Override
    public void removeToken(String tokenKey) {
        redisTemplate.delete(KEY_PREFIX + tokenKey);
    }
}
