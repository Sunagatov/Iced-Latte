package com.zufar.icedlatte.common.retry;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RetryAttemptSupport")
class RetryAttemptSupportTest {

    @Test
    @DisplayName("backs off exponentially with an upper bound")
    void backsOffExponentiallyWithUpperBound() {
        assertThat(RetryAttemptSupport.backoffSeconds(1)).isEqualTo(2);
        assertThat(RetryAttemptSupport.backoffSeconds(3)).isEqualTo(8);
        assertThat(RetryAttemptSupport.backoffSeconds(100)).isEqualTo(300);
    }

    @Test
    @DisplayName("calculates the next attempt in the future")
    void calculatesNextAttemptInTheFuture() {
        Instant before = Instant.now();

        Instant nextAttemptAt = RetryAttemptSupport.nextAttemptAt(2);

        assertThat(Duration.between(before, nextAttemptAt)).isGreaterThanOrEqualTo(Duration.ofSeconds(4));
    }

    @Test
    @DisplayName("bounds stored error text")
    void boundsStoredErrorText() {
        String message = "x".repeat(1200);

        String error = RetryAttemptSupport.sanitizedError(new IllegalStateException(message));

        assertThat(error).hasSize(1000);
        assertThat(error).startsWith("IllegalStateException: ");
    }
}
