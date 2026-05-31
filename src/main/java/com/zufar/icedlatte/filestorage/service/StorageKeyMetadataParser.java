package com.zufar.icedlatte.filestorage.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StorageKeyMetadataParser {

    Optional<FileMetadataDto> parse(String fileName, String bucketName) {
        String[] parts = fileName.split("/");
        String[] packageName = parts[0].split("_");
        if (packageName.length < 2) {
            log.warn("storage.key.skipped: key={}", fileName);
            return Optional.empty();
        }
        try {
            return Optional.of(new FileMetadataDto(UUID.fromString(packageName[1]), bucketName, fileName));
        } catch (IllegalArgumentException ex) {
            log.warn("storage.key.invalid_uuid: key={}", fileName);
            return Optional.empty();
        }
    }
}
