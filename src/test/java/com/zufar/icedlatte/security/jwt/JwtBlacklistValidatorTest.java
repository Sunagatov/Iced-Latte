package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistValidatorTest {

    @Mock
    private RedisJwtBlacklistService redisJwtBlacklistService;

    @InjectMocks
    private JwtBlacklistValidator jwtBlacklistValidator;

    private final String blacklistedToken = "blacklistedToken";

    @Test
    @DisplayName("Should blacklist token when added")
    void shouldBlacklistTokenWhenAdded() {
        String validToken = "validToken";
        jwtBlacklistValidator.addToBlacklist(validToken);
        verify(redisJwtBlacklistService).blacklistToken(validToken);
    }

    @Test
    @DisplayName("Should throw exception for blacklisted token")
    void shouldThrowExceptionForBlacklistedToken() {
        when(redisJwtBlacklistService.isBlacklisted(blacklistedToken)).thenReturn(true);
        assertThrows(JwtTokenBlacklistedException.class, () -> jwtBlacklistValidator.validate(blacklistedToken));
    }

    @Test
    @DisplayName("Should not throw exception for non-blacklisted token")
    void shouldNotThrowExceptionForNonBlacklistedToken() {
        when(redisJwtBlacklistService.isBlacklisted("someOtherToken")).thenReturn(false);
        assertDoesNotThrow(() -> jwtBlacklistValidator.validate("someOtherToken"));
    }
}
