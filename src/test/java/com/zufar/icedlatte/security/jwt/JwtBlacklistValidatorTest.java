package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private InMemoryJwtBlacklistService inMemoryJwtBlacklistService;

    private JwtBlacklistValidator jwtBlacklistValidator;

    private final String blacklistedToken = "blacklistedToken";

    @Test
    @DisplayName("Should blacklist token when Redis service is available")
    void shouldBlacklistTokenWhenRedisServiceAvailable() {
        jwtBlacklistValidator = new JwtBlacklistValidator(redisJwtBlacklistService, null);
        String validToken = "validToken";
        
        jwtBlacklistValidator.addToBlacklist(validToken);
        
        verify(redisJwtBlacklistService).blacklistToken(validToken);
    }

    @Test
    @DisplayName("Should blacklist token when only in-memory service is available")
    void shouldBlacklistTokenWhenInMemoryServiceAvailable() {
        jwtBlacklistValidator = new JwtBlacklistValidator(null, inMemoryJwtBlacklistService);
        String validToken = "validToken";
        
        jwtBlacklistValidator.addToBlacklist(validToken);
        
        verify(inMemoryJwtBlacklistService).blacklistToken(validToken);
    }

    @Test
    @DisplayName("Should throw exception for blacklisted token")
    void shouldThrowExceptionForBlacklistedToken() {
        jwtBlacklistValidator = new JwtBlacklistValidator(redisJwtBlacklistService, null);
        when(redisJwtBlacklistService.isBlacklisted(blacklistedToken)).thenReturn(true);
        
        assertThrows(JwtTokenBlacklistedException.class, () -> jwtBlacklistValidator.validate(blacklistedToken));
    }

    @Test
    @DisplayName("Should not throw exception for non-blacklisted token")
    void shouldNotThrowExceptionForNonBlacklistedToken() {
        jwtBlacklistValidator = new JwtBlacklistValidator(redisJwtBlacklistService, null);
        when(redisJwtBlacklistService.isBlacklisted("someOtherToken")).thenReturn(false);
        
        assertDoesNotThrow(() -> jwtBlacklistValidator.validate("someOtherToken"));
    }

    @Test
    @DisplayName("Should throw exception when no service is available")
    void shouldThrowExceptionWhenNoServiceAvailable() {
        jwtBlacklistValidator = new JwtBlacklistValidator(null, null);
        String validToken = "validToken";
        
        assertThrows(RuntimeException.class, () -> jwtBlacklistValidator.addToBlacklist(validToken));
    }
}
