package com.zufar.icedlatte.email.api.token;

import com.zufar.icedlatte.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenGenerator unit tests")
class TokenGeneratorTest {

    private final TokenGenerator tokenGenerator = new TokenGenerator();

    @Nested
    @DisplayName("nextToken")
    class NextToken {

        @RepeatedTest(10)
        @DisplayName("returns 9-digit numeric token")
        void returnsNineDigitNumericToken() {
            String token = tokenGenerator.nextToken();

            assertThat(token).hasSize(9).matches("\\d{9}");
        }

        @RepeatedTest(10)
        @DisplayName("always returns token accepted by validator")
        void alwaysReturnsTokenAcceptedByValidator() {
            assertThatCode(() -> tokenGenerator.tokenIsValid(tokenGenerator.nextToken()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("tokenIsValid")
    class TokenIsValid {

        @Test
        @DisplayName("accepts valid 9-digit token")
        void acceptsValidNineDigitToken() {
            assertThatCode(() -> tokenGenerator.tokenIsValid("123456789"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rejects token with wrong length")
        void rejectsTokenWithWrongLength() {
            assertThatThrownBy(() -> tokenGenerator.tokenIsValid("12345"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("rejects token with non-digit characters")
        void rejectsTokenWithNonDigitCharacters() {
            assertThatThrownBy(() -> tokenGenerator.tokenIsValid("12345678a"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("rejects empty token")
        void rejectsEmptyToken() {
            assertThatThrownBy(() -> tokenGenerator.tokenIsValid(""))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("rejects null token")
        void rejectsNullToken() {
            assertThatThrownBy(() -> tokenGenerator.tokenIsValid(null))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
