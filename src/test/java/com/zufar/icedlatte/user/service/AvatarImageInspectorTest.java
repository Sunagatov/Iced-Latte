package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;

@DisplayName("AvatarImageInspector unit tests")
class AvatarImageInspectorTest {

    private final AvatarImageInspector inspector = new AvatarImageInspector();

    @Test
    @DisplayName("inspects PNG avatar bytes")
    void inspectPngAvatarBytes() {
        AvatarImageInspection result =
                inspector.inspect(AvatarImageInspectorTestFixtures.png(96, 64), "image/png", 5_242_880L, 12_000_000L);

        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.width()).isEqualTo(96);
        assertThat(result.height()).isEqualTo(64);
        assertThat(result.sizeBytes()).isEqualTo(24);
    }

    @Test
    @DisplayName("inspects JPEG avatar bytes")
    void inspectJpegAvatarBytes() {
        AvatarImageInspection result = inspector.inspect(
                AvatarImageInspectorTestFixtures.jpeg(384, 192), "image/jpeg", 5_242_880L, 12_000_000L);

        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.width()).isEqualTo(384);
        assertThat(result.height()).isEqualTo(192);
    }

    @Test
    @DisplayName("inspects WebP avatar bytes")
    void inspectWebpAvatarBytes() {
        AvatarImageInspection result = inspector.inspect(
                AvatarImageInspectorTestFixtures.webpVp8x(128, 96), "image/webp", 5_242_880L, 12_000_000L);

        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.width()).isEqualTo(128);
        assertThat(result.height()).isEqualTo(96);
    }

    @Test
    @DisplayName("rejects actual image type mismatch")
    void inspectRejectsActualImageTypeMismatch() {
        assertThatThrownBy(() -> inspector.inspect(
                        AvatarImageInspectorTestFixtures.png(96, 64), "image/jpeg", 5_242_880L, 12_000_000L))
                .isInstanceOf(InvalidAvatarFileTypeException.class)
                .hasMessageContaining("image/jpeg");
    }

    @Test
    @DisplayName("rejects images above max byte count")
    void inspectRejectsImageAboveMaxBytes() {
        assertThatThrownBy(() ->
                        inspector.inspect(AvatarImageInspectorTestFixtures.png(96, 64), "image/png", 23L, 12_000_000L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload must be at most 23 bytes.");
    }

    @Test
    @DisplayName("rejects invalid image bytes")
    void inspectRejectsInvalidImageBytes() {
        assertThatThrownBy(() -> inspector.inspect("not-an-image".getBytes(), "image/png", 5_242_880L, 12_000_000L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload image bytes are invalid.");
    }

    @Test
    @DisplayName("rejects images above max pixel count")
    void inspectRejectsImageAboveMaxPixels() {
        assertThatThrownBy(() -> inspector.inspect(
                        AvatarImageInspectorTestFixtures.png(5000, 5000), "image/png", 5_242_880L, 12_000_000L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload image dimensions are too large.");
    }
}
