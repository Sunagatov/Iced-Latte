package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.user.config.AvatarUploadMode;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;

@DisplayName("AvatarUploadProcessor unit tests")
class AvatarUploadProcessorTest {

    private static final UUID USER_ID = UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99");
    private static final UUID UPLOAD_ID = UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431");

    private final AvatarUploadProcessor processor =
            new AvatarUploadProcessor(new AvatarUploadSourceObjectValidator(properties()), new AvatarImageInspector());

    @Test
    @DisplayName("validates source object and avatar image bytes")
    void processValidatesSourceObjectAndImageBytes() {
        AvatarUploadProcessingResult result = processor.process(
                sourceObject("image/png"), AvatarImageInspectorTestFixtures.png(96, 64), 5_242_880L, 12_000_000L);

        assertThat(result.source().userId()).isEqualTo(USER_ID);
        assertThat(result.source().uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(result.image().contentType()).isEqualTo("image/png");
        assertThat(result.image().width()).isEqualTo(96);
        assertThat(result.image().height()).isEqualTo(64);
    }

    @Test
    @DisplayName("rejects image bytes that do not match source metadata content type")
    void processRejectsImageBytesThatDoNotMatchMetadataContentType() {
        assertThatThrownBy(() -> processor.process(
                        sourceObject("image/jpeg"),
                        AvatarImageInspectorTestFixtures.png(96, 64),
                        5_242_880L,
                        12_000_000L))
                .isInstanceOf(InvalidAvatarFileTypeException.class);
    }

    private static AvatarUploadSourceObject sourceObject(String requestedContentType) {
        return new AvatarUploadSourceObject(
                "iced-latte-users",
                "avatars/incoming/%s/%s/source".formatted(USER_ID, UPLOAD_ID),
                Map.of(
                        "upload-id",
                        UPLOAD_ID.toString(),
                        "user-id",
                        USER_ID.toString(),
                        "requested-content-type",
                        requestedContentType,
                        "avatar-upload-version",
                        "1"));
    }

    private static AvatarUploadProperties properties() {
        return new AvatarUploadProperties(
                AvatarUploadMode.PRESIGNED,
                java.time.Duration.ofMinutes(5),
                5_242_880L,
                12_000_000L,
                java.time.Duration.ofMinutes(10),
                "iced-latte-users",
                "iced-latte-users",
                "");
    }
}
