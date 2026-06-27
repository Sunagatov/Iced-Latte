package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadMode;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;

@DisplayName("AvatarUploadCompletionValidator unit tests")
class AvatarUploadCompletionValidatorTest {

    private static final UUID USER_ID = UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99");
    private static final UUID UPLOAD_ID = UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431");

    private final AvatarUploadCompletionValidator validator =
            new AvatarUploadCompletionValidator(new AvatarUploadSourceObjectValidator(properties()), properties());

    @Test
    @DisplayName("accepts valid Lambda completion payload")
    void validateAcceptsValidCompletionPayload() {
        AvatarUploadCompletion result = validator.validate(command());

        assertThat(result.source().userId()).isEqualTo(USER_ID);
        assertThat(result.source().uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(result.processedBucket()).isEqualTo("iced-latte-users");
        assertThat(result.processedKey())
                .isEqualTo("avatars/processed/%s/%s/avatar.webp".formatted(USER_ID, UPLOAD_ID));
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.width()).isEqualTo(384);
        assertThat(result.height()).isEqualTo(384);
        assertThat(result.originalSizeBytes()).isEqualTo(1024L);
        assertThat(result.processedSizeBytes()).isEqualTo(512L);
        assertThat(result.sha256()).isEqualTo("a".repeat(64));
    }

    @Test
    @DisplayName("rejects completion without processed object bucket")
    void validateRejectsMissingProcessedBucket() {
        assertThatThrownBy(() -> validator.validate(command("", processedKey(), "image/webp", 384)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion processedBucket is required.");
    }

    @Test
    @DisplayName("rejects completion with unexpected processed bucket")
    void validateRejectsUnexpectedProcessedBucket() {
        assertThatThrownBy(() -> validator.validate(command("other-bucket", processedKey(), "image/webp", 384)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion processedBucket is invalid.");
    }

    @Test
    @DisplayName("rejects processed key that does not belong to source metadata")
    void validateRejectsProcessedKeyMismatch() {
        assertThatThrownBy(() -> validator.validate(command(
                        "iced-latte-users",
                        "avatars/processed/%s/%s/avatar.webp".formatted(UUID.randomUUID(), UPLOAD_ID),
                        "image/webp",
                        384)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion processedKey does not match source metadata.");
    }

    @Test
    @DisplayName("rejects unsupported processed content type")
    void validateRejectsUnsupportedContentType() {
        assertThatThrownBy(() -> validator.validate(command("iced-latte-users", processedKey(), "image/gif", 384)))
                .isInstanceOf(InvalidAvatarFileTypeException.class);
    }

    @Test
    @DisplayName("rejects non-positive image dimensions")
    void validateRejectsNonPositiveDimensions() {
        assertThatThrownBy(() -> validator.validate(command("iced-latte-users", processedKey(), "image/webp", 0)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion width must be positive.");
    }

    @Test
    @DisplayName("rejects invalid sha256")
    void validateRejectsInvalidSha256() {
        AvatarUploadCompletionCommand command = new AvatarUploadCompletionCommand(
                sourceObject(),
                "iced-latte-users",
                processedKey(),
                "image/webp",
                384,
                384,
                1024L,
                512L,
                "z".repeat(64));

        assertThatThrownBy(() -> validator.validate(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion sha256 is invalid.");
    }

    private static AvatarUploadCompletionCommand command() {
        return command("iced-latte-users", processedKey(), "image/webp", 384);
    }

    private static AvatarUploadCompletionCommand command(
            String processedBucket, String processedKey, String contentType, Integer width) {
        return new AvatarUploadCompletionCommand(
                sourceObject(), processedBucket, processedKey, contentType, width, 384, 1024L, 512L, "A".repeat(64));
    }

    private static AvatarUploadSourceObject sourceObject() {
        return new AvatarUploadSourceObject(
                "iced-latte-users",
                "avatars/incoming/%s/%s/source".formatted(USER_ID, UPLOAD_ID),
                Map.of(
                        "upload-id",
                        UPLOAD_ID.toString(),
                        "user-id",
                        USER_ID.toString(),
                        "requested-content-type",
                        "image/png",
                        "avatar-upload-version",
                        "1"));
    }

    private static String processedKey() {
        return "avatars/processed/%s/%s/avatar.webp".formatted(USER_ID, UPLOAD_ID);
    }

    private static AvatarUploadProperties properties() {
        return new AvatarUploadProperties(
                AvatarUploadMode.PRESIGNED,
                Duration.ofMinutes(5),
                5_242_880L,
                12_000_000L,
                Duration.ofMinutes(10),
                "iced-latte-users",
                "iced-latte-users",
                "");
    }
}
