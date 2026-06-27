package com.zufar.icedlatte.user.service;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.user.config.AvatarUploadProperties;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AvatarUploadCompletionValidator {

    private static final int SHA256_HEX_LENGTH = 64;
    private final AvatarUploadSourceObjectValidator sourceObjectValidator;
    private final AvatarUploadProperties properties;

    public AvatarUploadCompletion validate(AvatarUploadCompletionCommand command) {
        if (command == null) {
            throw new BadRequestException("Avatar upload completion is required.");
        }

        ValidAvatarUploadSourceObject source = sourceObjectValidator.validate(command.sourceObject());
        validateRequiredText(command.processedBucket(), "processedBucket");
        validateProcessedBucket(command.processedBucket());
        validateRequiredText(command.processedKey(), "processedKey");
        validateContentType(command.contentType());
        validateProcessedKey(source, command.processedKey());
        int width = positiveInt(command.width(), "width");
        int height = positiveInt(command.height(), "height");
        long originalSizeBytes = positiveLong(command.originalSizeBytes(), "originalSizeBytes");
        long processedSizeBytes = positiveLong(command.processedSizeBytes(), "processedSizeBytes");
        String sha256 = validateSha256(command.sha256());

        return new AvatarUploadCompletion(
                source,
                command.processedBucket(),
                command.processedKey(),
                command.contentType(),
                width,
                height,
                originalSizeBytes,
                processedSizeBytes,
                sha256);
    }

    private void validateContentType(String contentType) {
        validateRequiredText(contentType, "contentType");
        if (!AvatarContentTypes.ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidAvatarFileTypeException(contentType, AvatarContentTypes.ALLOWED_CONTENT_TYPES);
        }
    }

    private void validateProcessedKey(ValidAvatarUploadSourceObject source, String processedKey) {
        String expectedPrefix = AvatarUploadStorageLayout.processedPrefix(source.userId(), source.uploadId());
        if (!processedKey.startsWith(expectedPrefix)) {
            throw new BadRequestException("Avatar upload completion processedKey does not match source metadata.");
        }
    }

    private void validateProcessedBucket(String processedBucket) {
        if (!properties.processedBucket().equals(processedBucket)) {
            throw new BadRequestException("Avatar upload completion processedBucket is invalid.");
        }
    }

    private int positiveInt(Integer value, String fieldName) {
        if (value == null || value < 1) {
            throw new BadRequestException("Avatar upload completion " + fieldName + " must be positive.");
        }
        return value;
    }

    private long positiveLong(Long value, String fieldName) {
        if (value == null || value < 1) {
            throw new BadRequestException("Avatar upload completion " + fieldName + " must be positive.");
        }
        return value;
    }

    private String validateSha256(String sha256) {
        validateRequiredText(sha256, "sha256");
        if (sha256.length() != SHA256_HEX_LENGTH || !sha256.chars().allMatch(this::isHexDigit)) {
            throw new BadRequestException("Avatar upload completion sha256 is invalid.");
        }
        return sha256.toLowerCase(Locale.ROOT);
    }

    private boolean isHexDigit(int value) {
        return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
    }

    private void validateRequiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Avatar upload completion " + fieldName + " is required.");
        }
    }
}
