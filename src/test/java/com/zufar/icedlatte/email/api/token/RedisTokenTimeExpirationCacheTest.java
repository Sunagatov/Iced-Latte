package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.TimeTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisTokenTimeExpirationCache unit tests")
class RedisTokenTimeExpirationCacheTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("manageEmailSendingRate stores the expiry timestamp with TTL")
    void manageEmailSendingRateStoresExpiryTimestampWithTtl() {
        RedisTokenTimeExpirationCache cache = cache();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.manageEmailSendingRate("alice@example.com");

        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("email:rate:alice@example.com"),
                valueCaptor.capture(), org.mockito.ArgumentMatchers.eq(Duration.ofMinutes(15)));
        assertThat(OffsetDateTime.parse(valueCaptor.getValue()))
                .isAfter(OffsetDateTime.now().plusMinutes(14));
    }

    @Test
    @DisplayName("validateTimeToken is a no-op when no cooldown exists")
    void validateTimeTokenIsNoOpWhenNoCooldownExists() {
        RedisTokenTimeExpirationCache cache = cache();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:rate:alice@example.com")).thenReturn(null);

        assertThatCode(() -> cache.validateTimeToken("alice@example.com")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateTimeToken throws with the stored expiry when cooldown exists")
    void validateTimeTokenThrowsWithStoredExpiryWhenCooldownExists() {
        RedisTokenTimeExpirationCache cache = cache();
        OffsetDateTime expiry = OffsetDateTime.now().plusMinutes(5);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:rate:alice@example.com")).thenReturn(expiry.toString());

        assertThatThrownBy(() -> cache.validateTimeToken("alice@example.com"))
                .isInstanceOf(TimeTokenException.class)
                .hasMessageContaining("alice@example.com");
    }

    @Test
    @DisplayName("removeToken deletes the namespaced rate-limit key")
    void removeTokenDeletesNamespacedRateLimitKey() {
        RedisTokenTimeExpirationCache cache = cache();

        cache.removeToken("alice@example.com");

        verify(redisTemplate).delete("email:rate:alice@example.com");
    }

    private RedisTokenTimeExpirationCache cache() {
        RedisTokenTimeExpirationCache cache = new RedisTokenTimeExpirationCache(redisTemplate);
        ReflectionTestUtils.setField(cache, "expireTimeMinutes", 15);
        return cache;
    }
}
