package com.zufar.icedlatte.email.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidTokenException")
class InvalidTokenExceptionTest {

    @Test
    @DisplayName("keeps the email in the exception message")
    void keepsEmailInMessage() {
        InvalidTokenException exception = new InvalidTokenException("user@example.com");

        assertThat(exception).hasMessage("Invalid token for email = user@example.com");
    }
}
