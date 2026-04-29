package com.zufar.icedlatte.security.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenHasNoUserEmailException")
class JwtTokenHasNoUserEmailExceptionTest {

    @Test
    @DisplayName("is an authentication exception with message")
    void isAuthenticationExceptionWithMessage() {
        JwtTokenHasNoUserEmailException exception =
                new JwtTokenHasNoUserEmailException("Missing email in JWT token");

        assertThat(exception)
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Missing email in JWT token");
    }

    @Test
    @DisplayName("message and cause constructor preserves both")
    void messageAndCauseConstructorPreservesBoth() {
        RuntimeException cause = new RuntimeException("root");
        JwtTokenHasNoUserEmailException exception =
                new JwtTokenHasNoUserEmailException("Missing email in JWT token", cause);

        assertThat(exception).hasMessage("Missing email in JWT token").hasCause(cause);
    }
}
