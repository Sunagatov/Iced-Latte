package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.exception.BadRequestException;

@DisplayName("AvatarUploadCompletionQueueMessageParser unit tests")
class AvatarUploadCompletionQueueMessageParserTest {

    private static final UUID USER_ID = UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99");
    private static final UUID UPLOAD_ID = UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431");

    private final AvatarUploadCompletionQueueMessageParser parser =
            new AvatarUploadCompletionQueueMessageParser(new ObjectMapper());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("maps ready SQS payload to completion command")
    void parseMapsReadyPayloadToCompletionCommand() {
        AvatarUploadCompletionQueueMessage message = parser.parse(readyPayload());

        assertThat(message.ready()).isTrue();
        AvatarUploadCompletionCommand command = message.completionCommand();
        assertThat(command.sourceObject().bucket()).isEqualTo("iced-latte-users");
        assertThat(command.sourceObject().key())
                .isEqualTo("avatars/incoming/%s/%s/source".formatted(USER_ID, UPLOAD_ID));
        assertThat(command.sourceObject().metadata())
                .containsEntry("upload-id", UPLOAD_ID.toString())
                .containsEntry("user-id", USER_ID.toString())
                .containsEntry("requested-content-type", "image/png")
                .containsEntry("avatar-upload-version", "1");
        assertThat(command.processedBucket()).isEqualTo("iced-latte-users");
        assertThat(command.processedKey())
                .isEqualTo("avatars/processed/%s/%s/avatar-384.webp".formatted(USER_ID, UPLOAD_ID));
        assertThat(command.contentType()).isEqualTo("image/webp");
        assertThat(command.width()).isEqualTo(384);
        assertThat(command.height()).isEqualTo(384);
        assertThat(command.originalSizeBytes()).isEqualTo(2048L);
        assertThat(command.processedSizeBytes()).isEqualTo(512L);
        assertThat(command.sha256()).isEqualTo("a".repeat(64));
    }

    @Test
    @DisplayName("maps failed SQS payload to failure command")
    void parseMapsFailedPayloadToFailureCommand() {
        AvatarUploadCompletionQueueMessage message = parser.parse(failedPayload());

        assertThat(message.ready()).isFalse();
        AvatarUploadFailureCommand command = message.failureCommand();
        assertThat(command.sourceObject().metadata()).containsEntry("upload-id", UPLOAD_ID.toString());
        assertThat(command.failureCode()).isEqualTo("DECODE_FAILED");
        assertThat(command.failureMessage()).isEqualTo("Cannot decode image");
    }

    @Test
    @DisplayName("rejects unsupported completion event version")
    void parseRejectsUnsupportedVersion() {
        AvatarUploadCompletionPayload payload = new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                2,
                USER_ID.toString(),
                UPLOAD_ID.toString(),
                "READY",
                "iced-latte-users",
                AvatarUploadStorageLayout.incomingKey(USER_ID, UPLOAD_ID),
                "image/png",
                "iced-latte-users",
                AvatarUploadStorageLayout.processedPrefix(USER_ID, UPLOAD_ID) + "avatar-384.webp",
                "image/webp",
                384,
                384,
                2048L,
                512L,
                "a".repeat(64),
                null,
                null);

        assertThatThrownBy(() -> parser.parse(writeJson(payload)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion message version is unsupported.");
    }

    private String readyPayload() {
        return writeJson(AvatarUploadCompletionPayloadTest.readyPayload());
    }

    private String failedPayload() {
        return writeJson(new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                AvatarUploadCompletionPayload.VERSION,
                USER_ID.toString(),
                UPLOAD_ID.toString(),
                "FAILED",
                "iced-latte-users",
                AvatarUploadStorageLayout.incomingKey(USER_ID, UPLOAD_ID),
                "image/png",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "DECODE_FAILED",
                "Cannot decode image"));
    }

    private String writeJson(AvatarUploadCompletionPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
