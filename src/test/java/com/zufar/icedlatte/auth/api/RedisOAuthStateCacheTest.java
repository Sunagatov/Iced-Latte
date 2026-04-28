package com.zufar.icedlatte.auth.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisOAuthStateCache unit tests")
class RedisOAuthStateCacheTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RedisOAuthStateCache cache;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cache = new RedisOAuthStateCache(redisTemplate);
        ReflectionTestUtils.setField(cache, "ttlMinutes", 10);
    }

    @Nested
    @DisplayName("store")
    class Store {

        @Test
        @DisplayName("stores the callback under the prefixed key with configured TTL")
        void storesCallbackUnderPrefixedKeyWithConfiguredTtl() {
            cache.store("nonce-1", "https://example.com/callback");

            verify(redisTemplate).opsForValue();
            verify(valueOperations).set(
                    "oauth:state:nonce-1",
                    "https://example.com/callback",
                    Duration.ofMinutes(10)
            );
            verifyNoMoreInteractions(redisTemplate, valueOperations);
        }
    }

    @Nested
    @DisplayName("consume")
    class Consume {

        @Test
        @DisplayName("returns and deletes the stored callback")
        void returnsAndDeletesStoredCallback() {
            when(valueOperations.getAndDelete("oauth:state:nonce-1"))
                    .thenReturn("https://example.com/callback");

            String result = cache.consume("nonce-1");

            assertThat(result).isEqualTo("https://example.com/callback");
            verify(redisTemplate).opsForValue();
            verify(valueOperations).getAndDelete("oauth:state:nonce-1");
            verifyNoMoreInteractions(redisTemplate, valueOperations);
        }

        @Test
        @DisplayName("returns null when the nonce is absent")
        void returnsNullWhenNonceIsAbsent() {
            when(valueOperations.getAndDelete("oauth:state:missing")).thenReturn(null);

            String result = cache.consume("missing");

            assertThat(result).isNull();
            verify(redisTemplate).opsForValue();
            verify(valueOperations).getAndDelete("oauth:state:missing");
            verifyNoMoreInteractions(redisTemplate, valueOperations);
        }
    }
}
