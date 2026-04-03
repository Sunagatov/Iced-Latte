package com.zufar.icedlatte.auth.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisOAuthStateCache unit tests")
class RedisOAuthStateCacheTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private RedisOAuthStateCache cache;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cache = new RedisOAuthStateCache(redisTemplate);
        ReflectionTestUtils.setField(cache, "ttlMinutes", 10);
    }

    @Test
    @DisplayName("store delegates to Redis with correct key and TTL")
    void storeDelegatesToRedis() {
        cache.store("nonce1", "https://example.com/cb");
        verify(valueOps).set("oauth:state:nonce1", "https://example.com/cb", Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("consume returns value and deletes key when present")
    void consumeReturnValueAndDeletes() {
        when(valueOps.get("oauth:state:nonce1")).thenReturn("https://example.com/cb");
        String result = cache.consume("nonce1");
        assertThat(result).isEqualTo("https://example.com/cb");
        verify(redisTemplate).delete("oauth:state:nonce1");
    }

    @Test
    @DisplayName("consume returns null and does not delete when key absent")
    void consumeReturnsNullWhenAbsent() {
        when(valueOps.get("oauth:state:missing")).thenReturn(null);
        String result = cache.consume("missing");
        assertThat(result).isNull();
        verify(redisTemplate, never()).delete(anyString());
    }
}
