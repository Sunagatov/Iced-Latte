package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.email.exception.IncorrectTokenFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenGenerator unit tests")
class TokenGeneratorTest {

    private final TokenGenerator tokenGenerator = new TokenGenerator();

    @RepeatedTest(10)
    @DisplayName("Generated token is 9 digits")
    void nextToken_alwaysReturns9DigitString() {
        String token = tokenGenerator.nextToken();
        assertThat(token).hasSize(9).matches("\\d{9}");
    }

    @Test
    @DisplayName("Valid 9-digit token passes validation")
    void tokenIsValid_validToken_doesNotThrow() {
        tokenGenerator.tokenIsValid("123456789");
    }

    @Test
    @DisplayName("Token with wrong length throws IncorrectTokenFormatException")
    void tokenIsValid_wrongLength_throws() {
        assertThatThrownBy(() -> tokenGenerator.tokenIsValid("12345"))
                .isInstanceOf(IncorrectTokenFormatException.class);
    }

    @Test
    @DisplayName("Token with non-digit characters throws IncorrectTokenFormatException")
    void tokenIsValid_nonDigitChars_throws() {
        assertThatThrownBy(() -> tokenGenerator.tokenIsValid("12345678a"))
                .isInstanceOf(IncorrectTokenFormatException.class);
    }

    @Test
    @DisplayName("Empty token throws IncorrectTokenFormatException")
    void tokenIsValid_emptyString_throws() {
        assertThatThrownBy(() -> tokenGenerator.tokenIsValid(""))
                .isInstanceOf(IncorrectTokenFormatException.class);
    }

    @Test
    @DisplayName("Generated token passes its own validation")
    void nextToken_generatedTokenPassesValidation() {
        String token = tokenGenerator.nextToken();
        // should not throw
        tokenGenerator.tokenIsValid(token);
    }
}
