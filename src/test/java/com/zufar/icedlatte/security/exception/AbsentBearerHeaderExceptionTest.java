package com.zufar.icedlatte.security.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AbsentBearerHeaderException")
class AbsentBearerHeaderExceptionTest {

    @Test
    @DisplayName("default constructor uses standard message")
    void defaultConstructorUsesStandardMessage() {
        assertThat(new AbsentBearerHeaderException())
                .hasMessage("Bearer authentication header is absent");
    }

    @Test
    @DisplayName("message constructor preserves custom message")
    void messageConstructorPreservesCustomMessage() {
        assertThat(new AbsentBearerHeaderException("Custom message"))
                .hasMessage("Custom message");
    }

    @Test
    @DisplayName("message and cause constructor preserves both")
    void messageAndCauseConstructorPreservesBoth() {
        RuntimeException cause = new RuntimeException("root");
        AbsentBearerHeaderException exception = new AbsentBearerHeaderException("Custom message", cause);

        assertThat(exception).hasMessage("Custom message").hasCause(cause);
    }
}
