package com.zufar.icedlatte.common.temporarycache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisExpiringKeyValueStore unit tests")
class RedisExpiringKeyValueStoreTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("put stores the raw string value with TTL")
    void putStoresTheRawStringValueWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisExpiringKeyValueStore store = new RedisExpiringKeyValueStore(redisTemplate);

        store.put("key", "value", Duration.ofMinutes(5));

        verify(valueOperations).set("key", "value", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("get returns stored values")
    void getReturnsStoredValues() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("key")).thenReturn("value");
        RedisExpiringKeyValueStore store = new RedisExpiringKeyValueStore(redisTemplate);

        assertThat(store.get("key")).contains("value");
    }

    @Test
    @DisplayName("take returns and removes stored values")
    void takeReturnsAndRemovesStoredValues() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("key")).thenReturn("value");
        RedisExpiringKeyValueStore store = new RedisExpiringKeyValueStore(redisTemplate);

        assertThat(store.take("key")).contains("value");
    }

    @Test
    @DisplayName("remove deletes the key")
    void removeDeletesKey() {
        RedisExpiringKeyValueStore store = new RedisExpiringKeyValueStore(redisTemplate);

        store.remove("key");

        verify(redisTemplate).delete("key");
    }

    @Test
    @DisplayName("contains delegates to Redis hasKey")
    void containsDelegatesToRedisHasKey() {
        when(redisTemplate.hasKey("key")).thenReturn(true);
        RedisExpiringKeyValueStore store = new RedisExpiringKeyValueStore(redisTemplate);

        assertThat(store.contains("key")).isTrue();
        verify(redisTemplate).hasKey(eq("key"));
    }
}
