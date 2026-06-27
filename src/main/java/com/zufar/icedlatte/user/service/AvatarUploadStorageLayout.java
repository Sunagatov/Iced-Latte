package com.zufar.icedlatte.user.service;

import java.util.Map;
import java.util.UUID;

public final class AvatarUploadStorageLayout {

    static final String CURRENT_METADATA_VERSION = "1";
    static final String UPLOAD_ID_METADATA = "upload-id";
    static final String USER_ID_METADATA = "user-id";
    static final String REQUESTED_CONTENT_TYPE_METADATA = "requested-content-type";
    static final String VERSION_METADATA = "avatar-upload-version";

    private AvatarUploadStorageLayout() {}

    public static String incomingKey(UUID userId, UUID uploadId) {
        return "avatars/incoming/%s/%s/source".formatted(userId, uploadId);
    }

    public static String processedPrefix(UUID userId, UUID uploadId) {
        return "avatars/processed/%s/%s/".formatted(userId, uploadId);
    }

    public static Map<String, String> sourceMetadata(UUID userId, UUID uploadId, String requestedContentType) {
        return sourceMetadata(userId.toString(), uploadId.toString(), requestedContentType);
    }

    public static Map<String, String> sourceMetadata(String userId, String uploadId, String requestedContentType) {
        return Map.of(
                UPLOAD_ID_METADATA,
                uploadId,
                USER_ID_METADATA,
                userId,
                REQUESTED_CONTENT_TYPE_METADATA,
                requestedContentType,
                VERSION_METADATA,
                CURRENT_METADATA_VERSION);
    }
}
