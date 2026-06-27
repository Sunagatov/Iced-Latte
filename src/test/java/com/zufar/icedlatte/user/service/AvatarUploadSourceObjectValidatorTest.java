package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadMode;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;

@DisplayName("AvatarUploadSourceObjectValidator unit tests")
class AvatarUploadSourceObjectValidatorTest {

    private static final UUID USER_ID = UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99");
    private static final UUID UPLOAD_ID = UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431");
    private static final String BUCKET = "iced-latte-users";
    private static final String KEY = "avatars/incoming/%s/%s/source".formatted(USER_ID, UPLOAD_ID);

    private final AvatarUploadSourceObjectValidator validator = new AvatarUploadSourceObjectValidator(properties());

    @Test
    @DisplayName("accepts matching avatar upload source object metadata")
    void validateAcceptsMatchingMetadata() {
        AvatarUploadSourceObject object = new AvatarUploadSourceObject(BUCKET, KEY, metadata());

        ValidAvatarUploadSourceObject result = validator.validate(object);

        assertThat(result.bucket()).isEqualTo(BUCKET);
        assertThat(result.key()).isEqualTo(KEY);
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.uploadId()).isEqualTo(UPLOAD_ID);
        assertThat(result.requestedContentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("rejects source object when required metadata is missing")
    void validateRejectsMissingMetadata() {
        AvatarUploadSourceObject object =
                new AvatarUploadSourceObject(BUCKET, KEY, Map.of("upload-id", UPLOAD_ID.toString()));

        assertThatThrownBy(() -> validator.validate(object))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload source object metadata is incomplete.");
    }

    @Test
    @DisplayName("rejects source object from unexpected bucket")
    void validateRejectsUnexpectedBucket() {
        AvatarUploadSourceObject object = new AvatarUploadSourceObject("other-bucket", KEY, metadata());

        assertThatThrownBy(() -> validator.validate(object))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload source object bucket is invalid.");
    }

    @Test
    @DisplayName("rejects source object when metadata identifiers do not match key")
    void validateRejectsMetadataKeyMismatch() {
        AvatarUploadSourceObject object = new AvatarUploadSourceObject(
                BUCKET,
                KEY,
                Map.of(
                        "upload-id",
                        UUID.randomUUID().toString(),
                        "user-id",
                        USER_ID.toString(),
                        "requested-content-type",
                        "image/png",
                        "avatar-upload-version",
                        "1"));

        assertThatThrownBy(() -> validator.validate(object))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload source object key does not match metadata.");
    }

    @Test
    @DisplayName("rejects unsupported avatar upload metadata version")
    void validateRejectsUnsupportedVersion() {
        AvatarUploadSourceObject object = new AvatarUploadSourceObject(
                BUCKET,
                KEY,
                Map.of(
                        "upload-id",
                        UPLOAD_ID.toString(),
                        "user-id",
                        USER_ID.toString(),
                        "requested-content-type",
                        "image/png",
                        "avatar-upload-version",
                        "2"));

        assertThatThrownBy(() -> validator.validate(object))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload source object metadata version is unsupported.");
    }

    @Test
    @DisplayName("rejects unsupported requested content type")
    void validateRejectsUnsupportedRequestedContentType() {
        AvatarUploadSourceObject object = new AvatarUploadSourceObject(
                BUCKET,
                KEY,
                Map.of(
                        "upload-id",
                        UPLOAD_ID.toString(),
                        "user-id",
                        USER_ID.toString(),
                        "requested-content-type",
                        "image/gif",
                        "avatar-upload-version",
                        "1"));

        assertThatThrownBy(() -> validator.validate(object))
                .isInstanceOf(InvalidAvatarFileTypeException.class)
                .hasMessageContaining("Avatar file type not allowed: image/gif");
    }

    private static Map<String, String> metadata() {
        return Map.of(
                "upload-id",
                UPLOAD_ID.toString(),
                "user-id",
                USER_ID.toString(),
                "requested-content-type",
                "image/png",
                "avatar-upload-version",
                "1");
    }

    private static AvatarUploadProperties properties() {
        return new AvatarUploadProperties(
                AvatarUploadMode.PRESIGNED,
                java.time.Duration.ofMinutes(5),
                5_242_880L,
                12_000_000L,
                java.time.Duration.ofMinutes(10),
                BUCKET,
                BUCKET,
                "");
    }
}
