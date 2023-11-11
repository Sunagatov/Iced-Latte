package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.JwtTokenBlacklistedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtBlacklistValidatorTest {

    private JwtBlacklistValidator jwtBlacklistValidator = new JwtBlacklistValidator();
    private String token = "testToken";

    @Test
    @DisplayName("Given a token, When it is added to the blacklist, Then it should be in the blacklist")
    public void shouldAddTokenToBlacklist() throws Exception {
        Field blacklistedTokensField = JwtBlacklistValidator.class.getDeclaredField("blacklistedTokens");
        blacklistedTokensField.setAccessible(true);
        Set<String> blacklistedTokens = (Set<String>) blacklistedTokensField.get(jwtBlacklistValidator);

        jwtBlacklistValidator.addToBlacklist(token);

        assertTrue(blacklistedTokens.contains(token));
    }

    @Test
    @DisplayName("Given a blacklisted token, When it is validated, Then it should not throw JwtTokenBlacklistedException")
    public void shouldNotThrowExceptionForValidatedBlacklistedToken() {
        jwtBlacklistValidator.addToBlacklist(token);
        String tokenToValidate = "wrong token";
        assertDoesNotThrow(() -> jwtBlacklistValidator.validate(tokenToValidate));
    }

    @Test
    @DisplayName("Given a non-blacklisted token, When it is validated, Then it should throw JwtTokenBlacklistedException")
    public void shouldThrowExceptionForValidatedNonBlacklistedToken() {
        jwtBlacklistValidator.addToBlacklist(token);
        assertThrows(JwtTokenBlacklistedException.class, () -> {
            jwtBlacklistValidator.validate(token);
        });
    }
}