package com.zufar.icedlatte.security.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidCredentialsException")
class InvalidCredentialsExceptionTest {

    @Test
    @DisplayName("default constructor uses standard message")
    void defaultConstructorUsesStandardMessage() {
        assertThat(new InvalidCredentialsException()).hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("cause constructor preserves cause and standard message")
    void causeConstructorPreservesCauseAndMessage() {
        RuntimeException cause = new RuntimeException("root");

        InvalidCredentialsException exception = new InvalidCredentialsException(cause);

        assertThat(exception).hasMessage("Invalid credentials").hasCause(cause);
    }
}
