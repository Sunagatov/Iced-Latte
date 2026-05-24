package com.zufar.icedlatte.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Preconditions")
class PreconditionsTest {

    @Test
    @DisplayName("returns non-blank string")
    void returnsNonBlankString() {
        assertThat(Preconditions.requireNotBlankOrThrow("value", IllegalStateException::new))
                .isEqualTo("value");
    }

    @Test
    @DisplayName("throws for blank string")
    void throwsForBlankString() {
        assertThatThrownBy(() -> Preconditions.requireNotBlankOrThrow(" ", () -> new IllegalStateException("blank")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("blank");
    }

    @Test
    @DisplayName("returns positive integer")
    void returnsPositiveInteger() {
        assertThat(Preconditions.requirePositiveOrThrow(1, IllegalStateException::new))
                .isOne();
    }

    @Test
    @DisplayName("throws for non-positive integer")
    void throwsForNonPositiveInteger() {
        assertThatThrownBy(() -> Preconditions.requirePositiveOrThrow(0, () -> new IllegalStateException("positive")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("positive");
    }

    @Test
    @DisplayName("returns positive duration")
    void returnsPositiveDuration() {
        assertThat(Preconditions.requirePositiveOrThrow(Duration.ofSeconds(1), IllegalStateException::new))
                .isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("throws for non-positive duration")
    void throwsForNonPositiveDuration() {
        assertThatThrownBy(() -> Preconditions.requirePositiveOrThrow(
                        Duration.ZERO, () -> new IllegalStateException("positive duration")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("positive duration");
    }
}
