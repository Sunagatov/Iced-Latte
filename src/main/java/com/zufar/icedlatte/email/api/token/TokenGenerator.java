package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.IncorrectTokenFormatException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TokenGenerator {

    private static final int TOKEN_LENGTH = 9;
    private static final SecureRandom random = new SecureRandom();

    public String nextToken() {
        return String.format("%0" + TOKEN_LENGTH + "d", random.nextInt((int) Math.pow(10, TOKEN_LENGTH)));
    }

    public void tokenIsValid(String token) {
        if (token == null || token.length() != TOKEN_LENGTH || !token.chars().allMatch(Character::isDigit)) {
            throw new IncorrectTokenFormatException("#".repeat(TOKEN_LENGTH));
        }
    }
}
