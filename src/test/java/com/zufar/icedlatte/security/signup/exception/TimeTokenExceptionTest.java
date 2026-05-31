package com.zufar.icedlatte.security.signup.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TimeTokenException")
class TimeTokenExceptionTest {

    @Test
    @DisplayName("includes remaining time without exposing email in the message")
    void includesRemainingTimeWithoutExposingEmailInTheMessage() {
        TimeTokenException exception =
                new TimeTokenException(OffsetDateTime.now().plusSeconds(65));

        assertThat(exception.getMessage())
                .contains("Token will be expired after:")
                .doesNotContain("user@example.com")
                .contains("1 min")
                .contains("sec");
    }
}
