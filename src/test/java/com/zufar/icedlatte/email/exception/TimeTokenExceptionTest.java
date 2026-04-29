package com.zufar.icedlatte.email.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeTokenException")
class TimeTokenExceptionTest {

    @Test
    @DisplayName("includes email and remaining time in the message")
    void includesEmailAndRemainingTimeInTheMessage() {
        TimeTokenException exception = new TimeTokenException(
                "user@example.com",
                OffsetDateTime.now().plusSeconds(65)
        );

        assertThat(exception.getEmail()).isEqualTo("user@example.com");
        assertThat(exception.getMessage())
                .contains("Token for email 'user@example.com' will be expired after:")
                .contains("1 min")
                .contains("sec");
    }
}
