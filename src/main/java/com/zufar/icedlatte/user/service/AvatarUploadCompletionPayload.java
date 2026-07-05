package com.zufar.icedlatte.user.service;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;

public record AvatarUploadCompletionPayload(
        String eventType,
        @Nullable Integer version,
        String userId,
        String uploadId,
        String status,
        String sourceBucket,
        String sourceKey,
        String requestedContentType,
        @Nullable String processedBucket,
        @Nullable String processedKey,
        @Nullable String contentType,
        @Nullable Integer width,
        @Nullable Integer height,
        @Nullable Long originalSizeBytes,
        @Nullable Long processedSizeBytes,
        @Nullable String sha256,
        @Nullable String failureCode,
        @Nullable String failureMessage) {

    static final String EVENT_TYPE = "AvatarProcessed";
    static final int VERSION = 1;

    AvatarUploadCompletionQueueMessage toQueueMessage() {
        String normalizedStatus = requireText(status, "status");
        AvatarUploadSourceObject sourceObject = new AvatarUploadSourceObject(
                requireText(sourceBucket, "sourceBucket"),
                requireText(sourceKey, "sourceKey"),
                AvatarUploadStorageLayout.sourceMetadata(
                        requireText(userId, "userId"),
                        requireText(uploadId, "uploadId"),
                        requireText(requestedContentType, "requestedContentType")));
        return switch (normalizedStatus) {
            case "READY" ->
                AvatarUploadCompletionQueueMessage.ready(new AvatarUploadCompletionCommand(
                        sourceObject,
                        requireText(processedBucket, "processedBucket"),
                        requireText(processedKey, "processedKey"),
                        requireText(contentType, "contentType"),
                        requireValue(width, "width"),
                        requireValue(height, "height"),
                        requireValue(originalSizeBytes, "originalSizeBytes"),
                        requireValue(processedSizeBytes, "processedSizeBytes"),
                        requireText(sha256, "sha256")));
            case "FAILED" ->
                AvatarUploadCompletionQueueMessage.failed(new AvatarUploadFailureCommand(
                        sourceObject,
                        requireText(failureCode, "failureCode"),
                        requireText(failureMessage, "failureMessage")));
            default -> throw new BadRequestException("Avatar upload completion message status is unsupported.");
        };
    }

    private static String requireText(@Nullable String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Avatar upload completion message " + fieldName + " is required.");
        }
        return value;
    }

    private static <T> T requireValue(@Nullable T value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("Avatar upload completion message " + fieldName + " is required.");
        }
        return value;
    }
}
