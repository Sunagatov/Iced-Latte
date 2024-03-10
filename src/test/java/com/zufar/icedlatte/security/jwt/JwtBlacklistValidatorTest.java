package com.zufar.icedlatte.security.jwt;

import com.zufar.icedlatte.security.exception.JwtTokenBlacklistedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtBlacklistValidatorTest {

    private final JwtBlacklistValidator jwtBlacklistValidator = new JwtBlacklistValidator();
    private final String validToken = "validToken";
    private final String blacklistedToken = "blacklistedToken";

    @Test
    @DisplayName("Should blacklist token when added")
    void shouldBlacklistTokenWhenAdded() {
        jwtBlacklistValidator.addToBlacklist(validToken);
        assertThrows(JwtTokenBlacklistedException.class, () -> jwtBlacklistValidator.validate(validToken));
    }

    @Test
    @DisplayName("Should throw exception for blacklisted token")
    void shouldThrowExceptionForBlacklistedToken() {
        jwtBlacklistValidator.addToBlacklist(blacklistedToken);
        assertThrows(JwtTokenBlacklistedException.class, () -> jwtBlacklistValidator.validate(blacklistedToken));
    }

    @Test
    @DisplayName("Should not throw exception for non-blacklisted token")
    void shouldNotThrowExceptionForNonBlacklistedToken() {
        assertDoesNotThrow(() -> jwtBlacklistValidator.validate("someOtherToken"));
    }
}
