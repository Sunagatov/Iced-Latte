package com.zufar.icedlatte.email.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IncorrectTokenFormatException")
class IncorrectTokenFormatExceptionTest {

    @Test
    @DisplayName("describes the expected token format")
    void describesExpectedTokenFormat() {
        IncorrectTokenFormatException exception = new IncorrectTokenFormatException("UUID");

        assertThat(exception).hasMessage("Incorrect token format, token must be UUID");
    }
}
