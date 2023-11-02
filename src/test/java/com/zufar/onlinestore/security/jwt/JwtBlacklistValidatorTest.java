package com.zufar.onlinestore.security.jwt;

import com.zufar.onlinestore.security.exception.JwtTokenBlacklistedException;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtBlacklistValidatorTest {
    private JwtBlacklistValidator jwtBlacklistValidator = new JwtBlacklistValidator();
    private String token = Instancio.of(String.class).create();

    @Test
    public void testValidateNotBlacklisted() {
        jwtBlacklistValidator.addToBlacklist(token);
        assertThrows(JwtTokenBlacklistedException.class, () -> {
            jwtBlacklistValidator.validate(token);
        });
    }

    @Test
    public void testValidateBlacklisted() {
        jwtBlacklistValidator.addToBlacklist(token);
        String tokenToValidate = Instancio.of(String.class).create();
        assertDoesNotThrow(() -> jwtBlacklistValidator.validate(tokenToValidate));
    }

}