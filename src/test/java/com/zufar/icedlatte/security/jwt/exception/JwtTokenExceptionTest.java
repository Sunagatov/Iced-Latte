package com.zufar.icedlatte.security.jwt.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenException")
class JwtTokenExceptionTest {

    @Test
    @DisplayName("cause constructor preserves cause")
    void causeConstructorPreservesCause() {
        RuntimeException cause = new RuntimeException("root");

        JwtTokenException exception = new JwtTokenException(cause);

        assertThat(exception).hasCause(cause);
    }

    @Test
    @DisplayName("message constructor preserves message")
    void messageConstructorPreservesMessage() {
        assertThat(new JwtTokenException("Token invalid")).hasMessage("Token invalid");
    }

    @Test
    @DisplayName("message and cause constructor preserves both")
    void messageAndCauseConstructorPreservesBoth() {
        RuntimeException cause = new RuntimeException("root");
        JwtTokenException exception = new JwtTokenException("Token invalid", cause);

        assertThat(exception).hasMessage("Token invalid").hasCause(cause);
    }
}
