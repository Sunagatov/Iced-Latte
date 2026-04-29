package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.configuration.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisJwtBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RedisJwtBlacklistService redisJwtBlacklistService;

    private static final String TOKEN = "test.jwt.token";
    private static final Duration TTL = Duration.ofHours(1);

    private static final String BLACKLIST_KEY;

    static {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(TOKEN.getBytes(StandardCharsets.UTF_8));
            BLACKLIST_KEY = "jwt:blacklist:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Should blacklist token with TTL")
    void shouldBlacklistTokenWithTTL() {
        when(jwtProperties.expiration()).thenReturn(TTL);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        redisJwtBlacklistService.blacklistToken(TOKEN);

        verify(valueOperations).set(eq(BLACKLIST_KEY), eq("revoked"), any(Duration.class));
    }

    @Test
    @DisplayName("Should handle Redis failure during blacklist")
    void shouldHandleRedisFailureDuringBlacklist() {
        when(jwtProperties.expiration()).thenReturn(TTL);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(any(String.class), any(String.class), any(Duration.class));

        assertThatThrownBy(() -> redisJwtBlacklistService.blacklistToken(TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis connection failed");
    }

    @Test
    @DisplayName("Should ignore blank tokens during blacklist")
    void shouldIgnoreBlankTokensDuringBlacklist() {
        redisJwtBlacklistService.blacklistToken("   ");

        verify(redisTemplate, never()).opsForValue();
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("Should return true for blacklisted token")
    void shouldReturnTrueForBlacklistedToken() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(true);

        boolean result = redisJwtBlacklistService.isBlacklisted(TOKEN);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-blacklisted token")
    void shouldReturnFalseForNonBlacklistedToken() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(false);

        boolean result = redisJwtBlacklistService.isBlacklisted(TOKEN);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false when Redis returns null for hasKey")
    void shouldReturnFalseWhenRedisReturnsNullForHasKey() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenReturn(null);

        boolean result = redisJwtBlacklistService.isBlacklisted(TOKEN);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when Redis fails")
    void shouldReturnTrueWhenRedisFails() {
        when(redisTemplate.hasKey(BLACKLIST_KEY)).thenThrow(new RuntimeException("Redis down"));

        boolean result = redisJwtBlacklistService.isBlacklisted(TOKEN);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should treat blank token as blacklisted without Redis access")
    void shouldTreatBlankTokenAsBlacklistedWithoutRedisAccess() {
        assertThat(redisJwtBlacklistService.isBlacklisted("")).isTrue();
        verifyNoInteractions(redisTemplate, valueOperations);
    }
}
