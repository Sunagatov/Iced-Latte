package com.zufar.icedlatte.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailNormalizer")
class EmailNormalizerTest {

    @Test
    @DisplayName("normalizes email casing and surrounding whitespace")
    void normalizesEmailCasingAndSurroundingWhitespace() {
        assertThat(EmailNormalizer.normalize("  USER@Example.COM  ")).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("rejects null email")
    void rejectsNullEmail() {
        assertThatNullPointerException()
                .isThrownBy(() -> EmailNormalizer.normalize(null))
                .withMessage("email must not be null");
    }
}
