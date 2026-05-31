package com.zufar.icedlatte.filestorage.service;

import org.springframework.stereotype.Component;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FileMetadataSelectionPolicy {

    FileMetadataDto selectPreferred(FileMetadataDto first, FileMetadataDto second) {
        FileMetadataDto preferred = compare(first, second) <= 0 ? first : second;
        FileMetadataDto skipped = preferred == first ? second : first;
        log.warn(
                "storage.metadata.duplicate_related_object: objectId={}, selected={}, skipped={}",
                preferred.relatedObjectId(),
                preferred.fileName(),
                skipped.fileName());
        return preferred;
    }

    private int compare(FileMetadataDto first, FileMetadataDto second) {
        int rankComparison = Integer.compare(rank(first), rank(second));
        if (rankComparison != 0) {
            return rankComparison;
        }
        return first.fileName().compareTo(second.fileName());
    }

    private int rank(FileMetadataDto metadata) {
        String fileName = metadata.fileName().toLowerCase();
        String baseName = fileName.substring(fileName.lastIndexOf('/') + 1);
        switch (baseName) {
            case "card_logo.webp" -> {
                return 0;
            }
            case "card_logo.png" -> {
                return 1;
            }
            case "card_logo.jpg", "card_logo.jpeg" -> {
                return 2;
            }
        }
        if (fileName.endsWith(".webp")) {
            return 3;
        }
        if (fileName.endsWith(".png")) {
            return 4;
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return 5;
        }
        return 6;
    }
}
