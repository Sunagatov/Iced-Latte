package com.zufar.icedlatte.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidAvatarFileTypeException")
class InvalidAvatarFileTypeExceptionTest {

    private static final java.util.List<String> ALLOWED_TYPES = java.util.List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    @Test
    @DisplayName("renders invalid content type and allowed types")
    void rendersInvalidContentTypeAndAllowedTypes() {
        assertThat(new InvalidAvatarFileTypeException("image/gif", ALLOWED_TYPES))
                .hasMessage("Avatar file type not allowed: image/gif. Allowed types: image/jpeg, image/png, image/webp");
    }
}
