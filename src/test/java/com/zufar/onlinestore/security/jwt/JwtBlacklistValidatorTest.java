package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.JwtTokenBlacklistedException;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtBlacklistValidatorTest {
    private JwtBlacklistValidator jwtBlacklistValidator = new JwtBlacklistValidator();
    private String token = Instancio.of(String.class).create();

    @Test
    @DisplayName("test adding token to blacklist")
    public void testBlacklistedTokens() throws Exception {
        Field blacklistedTokensField = JwtBlacklistValidator.class.getDeclaredField("blacklistedTokens");
        blacklistedTokensField.setAccessible(true);

        Set<String> blacklistedTokens = (Set<String>) blacklistedTokensField.get(jwtBlacklistValidator);

        blacklistedTokens.add(token);
        assert (blacklistedTokens.contains(token));
    }
    @Test
    @DisplayName("test validate blacklisted token does not throw")
    public void testValidateBlacklisted() {
        jwtBlacklistValidator.addToBlacklist(token);
        String tokenToValidate = Instancio.of(String.class).create();
        assertDoesNotThrow(() -> jwtBlacklistValidator.validate(tokenToValidate));
    }

    @Test
    @DisplayName("test validate blacklisted token throw JwtTokenBlacklistedException")
    public void testValidateNotBlacklisted() {
        jwtBlacklistValidator.addToBlacklist(token);
        assertThrows(JwtTokenBlacklistedException.class, () -> {
            jwtBlacklistValidator.validate(token);
        });
    }

}