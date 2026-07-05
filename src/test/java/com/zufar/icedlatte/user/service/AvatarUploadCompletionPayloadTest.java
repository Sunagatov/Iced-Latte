package com.zufar.icedlatte.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zufar.icedlatte.common.exception.BadRequestException;

@DisplayName("AvatarUploadCompletionPayload unit tests")
class AvatarUploadCompletionPayloadTest {

    private static final UUID USER_ID = UUID.fromString("a4b55d63-f743-4154-a21e-aae91dba0b99");
    private static final UUID UPLOAD_ID = UUID.fromString("b67665b0-e1b8-4a58-b1f7-c71fd32a9431");

    @Test
    @DisplayName("maps ready payload to queue completion command")
    void toQueueMessageMapsReadyPayload() {
        AvatarUploadCompletionQueueMessage message = readyPayload().toQueueMessage();

        assertThat(message.ready()).isTrue();
        AvatarUploadCompletionCommand command = message.completionCommand();
        assertThat(command.sourceObject().bucket()).isEqualTo("iced-latte-users");
        assertThat(command.sourceObject().key()).isEqualTo(AvatarUploadStorageLayout.incomingKey(USER_ID, UPLOAD_ID));
        assertThat(command.sourceObject().metadata())
                .containsAllEntriesOf(AvatarUploadStorageLayout.sourceMetadata(USER_ID, UPLOAD_ID, "image/png"));
        assertThat(command.processedKey())
                .isEqualTo(AvatarUploadStorageLayout.processedPrefix(USER_ID, UPLOAD_ID) + "avatar-384.webp");
    }

    @Test
    @DisplayName("maps failed payload to queue failure command")
    void toQueueMessageMapsFailedPayload() {
        AvatarUploadCompletionQueueMessage message = failedPayload().toQueueMessage();

        assertThat(message.ready()).isFalse();
        AvatarUploadFailureCommand command = message.failureCommand();
        assertThat(command.sourceObject().metadata())
                .containsAllEntriesOf(AvatarUploadStorageLayout.sourceMetadata(USER_ID, UPLOAD_ID, "image/png"));
        assertThat(command.failureCode()).isEqualTo("DECODE_FAILED");
        assertThat(command.failureMessage()).isEqualTo("Cannot decode image");
    }

    @Test
    @DisplayName("rejects unsupported payload status")
    void toQueueMessageRejectsUnsupportedStatus() {
        AvatarUploadCompletionPayload payload = new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                AvatarUploadCompletionPayload.VERSION,
                USER_ID.toString(),
                UPLOAD_ID.toString(),
                "UNKNOWN",
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
                null,
                null);

        assertThatThrownBy(payload::toQueueMessage)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion message status is unsupported.");
    }

    @Test
    @DisplayName("rejects ready payload without processed object fields")
    void toQueueMessageRejectsReadyPayloadWithoutProcessedFields() {
        AvatarUploadCompletionPayload payload = new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                AvatarUploadCompletionPayload.VERSION,
                USER_ID.toString(),
                UPLOAD_ID.toString(),
                "READY",
                "iced-latte-users",
                AvatarUploadStorageLayout.incomingKey(USER_ID, UPLOAD_ID),
                "image/png",
                null,
                null,
                "image/webp",
                384,
                384,
                2048L,
                512L,
                "a".repeat(64),
                null,
                null);

        assertThatThrownBy(payload::toQueueMessage)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion message processedBucket is required.");
    }

    @Test
    @DisplayName("rejects failed payload without failure fields")
    void toQueueMessageRejectsFailedPayloadWithoutFailureFields() {
        AvatarUploadCompletionPayload payload = new AvatarUploadCompletionPayload(
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
                null,
                null);

        assertThatThrownBy(payload::toQueueMessage)
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Avatar upload completion message failureCode is required.");
    }

    @Test
    @DisplayName("rejects payload without user id before building source metadata")
    void toQueueMessageRejectsMissingUserId() {
        AvatarUploadCompletionPayload payload = new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                AvatarUploadCompletionPayload.VERSION,
                null,
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

        assertThatThrownBy(payload::toQueueMessage).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("rejects payload without upload id before building source metadata")
    void toQueueMessageRejectsMissingUploadId() {
        AvatarUploadCompletionPayload payload = new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                AvatarUploadCompletionPayload.VERSION,
                USER_ID.toString(),
                null,
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

        assertThatThrownBy(payload::toQueueMessage).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("rejects payload without requested content type before building source metadata")
    void toQueueMessageRejectsMissingRequestedContentType() {
        AvatarUploadCompletionPayload payload = new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                AvatarUploadCompletionPayload.VERSION,
                USER_ID.toString(),
                UPLOAD_ID.toString(),
                "READY",
                "iced-latte-users",
                AvatarUploadStorageLayout.incomingKey(USER_ID, UPLOAD_ID),
                null,
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

        assertThatThrownBy(payload::toQueueMessage).isInstanceOf(BadRequestException.class);
    }

    static AvatarUploadCompletionPayload readyPayload() {
        return new AvatarUploadCompletionPayload(
                AvatarUploadCompletionPayload.EVENT_TYPE,
                AvatarUploadCompletionPayload.VERSION,
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
    }

    private static AvatarUploadCompletionPayload failedPayload() {
        return new AvatarUploadCompletionPayload(
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
                "Cannot decode image");
    }
}
