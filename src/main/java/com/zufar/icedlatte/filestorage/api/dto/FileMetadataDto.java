package com.zufar.icedlatte.filestorage.api.dto;

import java.util.Objects;
import java.util.UUID;

public record FileMetadataDto(UUID relatedObjectId, String bucketName, String fileName) {

    public FileMetadataDto {
        Objects.requireNonNull(relatedObjectId, "relatedObjectId must not be null");
        requireHasText(bucketName, "bucketName");
        requireHasText(fileName, "fileName");
    }

    private static void requireHasText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
