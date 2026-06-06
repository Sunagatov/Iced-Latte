package com.zufar.icedlatte.filestorage.service;

import java.util.Optional;
import java.util.UUID;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
class StorageKeyMetadataParser {

    static Optional<FileMetadataDto> parse(String fileName, String bucketName) {
        int folderSeparatorIndex = fileName.indexOf('/');
        if (folderSeparatorIndex <= 0 || folderSeparatorIndex == fileName.length() - 1) {
            log.warn("storage.key.skipped: key={}", fileName);
            return Optional.empty();
        }

        String folderName = fileName.substring(0, folderSeparatorIndex);
        int uuidSeparatorIndex = folderName.lastIndexOf('_');
        if (uuidSeparatorIndex < 0 || uuidSeparatorIndex == folderName.length() - 1) {
            log.warn("storage.key.skipped: key={}", fileName);
            return Optional.empty();
        }
        try {
            UUID relatedObjectId = UUID.fromString(folderName.substring(uuidSeparatorIndex + 1));
            return Optional.of(new FileMetadataDto(relatedObjectId, bucketName, fileName));
        } catch (IllegalArgumentException ex) {
            log.warn("storage.key.invalid_uuid: key={}", fileName);
            return Optional.empty();
        }
    }
}
