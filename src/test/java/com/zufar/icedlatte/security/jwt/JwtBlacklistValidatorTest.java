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
    private JwtBlacklistService blacklistService;

    @InjectMocks
    private JwtBlacklistValidator jwtBlacklistValidator;

    @Test
    @DisplayName("Should delegate blacklisting to the blacklist service")
    void shouldBlacklistToken() {
        jwtBlacklistValidator.addToBlacklist("validToken");
        verify(blacklistService).blacklistToken("validToken");
    }

    @Test
    @DisplayName("Should throw exception for blacklisted token")
    void shouldThrowExceptionForBlacklistedToken() {
        when(blacklistService.isBlacklisted("blacklistedToken")).thenReturn(true);
        assertThrows(JwtTokenBlacklistedException.class, () -> jwtBlacklistValidator.validate("blacklistedToken"));
    }

    @Test
    @DisplayName("Should not throw exception for valid token")
    void shouldNotThrowExceptionForValidToken() {
        when(blacklistService.isBlacklisted("validToken")).thenReturn(false);
        assertDoesNotThrow(() -> jwtBlacklistValidator.validate("validToken"));
    }

    @Test
    @DisplayName("Should throw exception for empty token")
    void shouldThrowExceptionForEmptyToken() {
        assertThrows(JwtTokenBlacklistedException.class, () -> jwtBlacklistValidator.validate(""));
    }
}
