package com.zufar.icedlatte.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisJwtBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisJwtBlacklistService redisJwtBlacklistService;

    private final String token = "test.jwt.token";

    @Test
    @DisplayName("Should blacklist token with TTL")
    void shouldBlacklistTokenWithTTL() {
        ReflectionTestUtils.setField(redisJwtBlacklistService, "jwtTtl", Duration.ofHours(1));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisJwtBlacklistService.blacklistToken(token);

        verify(valueOperations).set(eq("jwt:blacklist:" + token.hashCode()), eq("revoked"), any(Duration.class));
    }

    @Test
    @DisplayName("Should handle Redis failure during blacklist")
    void shouldHandleRedisFailureDuringBlacklist() {
        ReflectionTestUtils.setField(redisJwtBlacklistService, "jwtTtl", Duration.ofHours(1));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(any(String.class), any(String.class), any(Duration.class));

        assertThrows(RuntimeException.class, () -> redisJwtBlacklistService.blacklistToken(token));
    }

    @Test
    @DisplayName("Should return true for blacklisted token")
    void shouldReturnTrueForBlacklistedToken() {
        when(redisTemplate.hasKey("jwt:blacklist:" + token.hashCode())).thenReturn(true);

        boolean result = redisJwtBlacklistService.isBlacklisted(token);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for non-blacklisted token")
    void shouldReturnFalseForNonBlacklistedToken() {
        when(redisTemplate.hasKey("jwt:blacklist:" + token.hashCode())).thenReturn(false);

        boolean result = redisJwtBlacklistService.isBlacklisted(token);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return true when Redis fails")
    void shouldReturnTrueWhenRedisFails() {
        when(redisTemplate.hasKey("jwt:blacklist:" + token.hashCode())).thenThrow(new RuntimeException("Redis down"));

        boolean result = redisJwtBlacklistService.isBlacklisted(token);

        assertTrue(result);
    }
}