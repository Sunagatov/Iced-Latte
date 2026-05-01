package com.zufar.icedlatte.common.temporarycache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisExpiringKeyValueStore implements ExpiringKeyValueStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize temporary cache value", e);
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> valueType) {
        String value = redisTemplate.opsForValue().get(key);
        return deserialize(value, valueType);
    }

    @Override
    public <T> Optional<T> take(String key, Class<T> valueType) {
        String value = redisTemplate.opsForValue().getAndDelete(key);
        return deserialize(value, valueType);
    }

    @Override
    public void remove(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean contains(String key) {
        Boolean hasKey = redisTemplate.hasKey(key);
        return hasKey != null && hasKey;
    }

    private <T> Optional<T> deserialize(String value, Class<T> valueType) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, valueType));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize temporary cache value", e);
        }
    }
}
