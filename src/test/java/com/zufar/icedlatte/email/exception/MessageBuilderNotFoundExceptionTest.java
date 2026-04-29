package com.zufar.icedlatte.email.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageBuilderNotFoundException")
class MessageBuilderNotFoundExceptionTest {

    @Test
    @DisplayName("retains the missing class name")
    void retainsMissingClassName() {
        MessageBuilderNotFoundException exception = new MessageBuilderNotFoundException("com.example.Event");

        assertThat(exception.getClassName()).isEqualTo("com.example.Event");
        assertThat(exception).hasMessage("No message builder found for com.example.Event");
    }
}
