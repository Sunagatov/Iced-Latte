package com.zufar.icedlatte.user.service;

import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AvatarUploadSourceObjectValidator {

    private final AvatarUploadProperties properties;

    public ValidAvatarUploadSourceObject validate(@Nullable AvatarUploadSourceObject object) {
        if (object == null || !StringUtils.hasText(object.bucket()) || !StringUtils.hasText(object.key())) {
            throw new BadRequestException("Avatar upload source object bucket and key are required.");
        }
        if (!properties.incomingBucket().equals(object.bucket())) {
            throw new BadRequestException("Avatar upload source object bucket is invalid.");
        }

        Map<String, String> metadata = object.metadata();
        if (metadata == null
                || missing(metadata, AvatarUploadStorageLayout.UPLOAD_ID_METADATA)
                || missing(metadata, AvatarUploadStorageLayout.USER_ID_METADATA)
                || missing(metadata, AvatarUploadStorageLayout.REQUESTED_CONTENT_TYPE_METADATA)
                || missing(metadata, AvatarUploadStorageLayout.VERSION_METADATA)) {
            throw new BadRequestException("Avatar upload source object metadata is incomplete.");
        }

        if (!AvatarUploadStorageLayout.CURRENT_METADATA_VERSION.equals(
                metadata.get(AvatarUploadStorageLayout.VERSION_METADATA))) {
            throw new BadRequestException("Avatar upload source object metadata version is unsupported.");
        }

        UUID uploadId = parseUuid(required(metadata, AvatarUploadStorageLayout.UPLOAD_ID_METADATA), "uploadId");
        UUID userId = parseUuid(required(metadata, AvatarUploadStorageLayout.USER_ID_METADATA), "userId");
        String requestedContentType = required(metadata, AvatarUploadStorageLayout.REQUESTED_CONTENT_TYPE_METADATA);
        if (!AvatarContentTypes.ALLOWED_CONTENT_TYPES.contains(requestedContentType)) {
            throw new InvalidAvatarFileTypeException(requestedContentType, AvatarContentTypes.ALLOWED_CONTENT_TYPES);
        }

        if (!AvatarUploadStorageLayout.incomingKey(userId, uploadId).equals(object.key())) {
            throw new BadRequestException("Avatar upload source object key does not match metadata.");
        }

        return new ValidAvatarUploadSourceObject(object.bucket(), object.key(), userId, uploadId, requestedContentType);
    }

    private boolean missing(Map<String, String> metadata, String key) {
        return !StringUtils.hasText(metadata.get(key));
    }

    private String required(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Avatar upload source object metadata is incomplete.");
        }
        return value;
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Avatar upload source object metadata " + fieldName + " is invalid.");
        }
    }
}
