package com.zufar.icedlatte.common.exception.handler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErrorDebugMessageCreator unit tests")
class ErrorDebugMessageCreatorTest {

    private final ErrorDebugMessageCreator creator = new ErrorDebugMessageCreator();

    @Test
    @DisplayName("Returns non-empty debug message for exception with stack trace")
    void buildErrorDebugMessage_exceptionWithStackTrace_returnsNonEmpty() {
        RuntimeException ex = new RuntimeException("test error");

        String message = creator.buildErrorDebugMessage(ex);

        assertThat(message).isNotBlank();
        assertThat(message).contains("class");
    }

    @Test
    @DisplayName("Debug message contains method name and line number")
    void buildErrorDebugMessage_containsMethodAndLine() {
        RuntimeException ex = new RuntimeException("test");

        String message = creator.buildErrorDebugMessage(ex);

        // Template: "Operation was failed in method: %s that belongs to the class: %s. Problematic code line: %d"
        assertThat(message).contains("Operation was failed in method:");
        assertThat(message).contains("that belongs to the class:");
        assertThat(message).contains("Problematic code line:");
    }

    @Test
    @DisplayName("Returns empty string for exception with no stack trace")
    void buildErrorDebugMessage_noStackTrace_returnsEmpty() {
        RuntimeException ex = new RuntimeException("no stack") {
            @Override
            public StackTraceElement[] getStackTrace() {
                return new StackTraceElement[0];
            }
        };

        String message = creator.buildErrorDebugMessage(ex);

        assertThat(message).isEmpty();
    }
}
