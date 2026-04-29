package com.zufar.icedlatte.security.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRegistrationException")
class UserRegistrationExceptionTest {

    @Test
    @DisplayName("message constructor preserves message")
    void messageConstructorPreservesMessage() {
        assertThat(new UserRegistrationException("Registration failed"))
                .hasMessage("Registration failed");
    }

    @Test
    @DisplayName("message and cause constructor preserves both")
    void messageAndCauseConstructorPreservesBoth() {
        RuntimeException cause = new RuntimeException("root");
        UserRegistrationException exception = new UserRegistrationException("Registration failed", cause);

        assertThat(exception).hasMessage("Registration failed").hasCause(cause);
    }
}
