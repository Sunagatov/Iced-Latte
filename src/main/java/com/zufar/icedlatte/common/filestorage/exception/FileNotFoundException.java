package com.zufar.icedlatte.common.filestorage.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class FileNotFoundException extends RuntimeException {

    private final UUID relatedObjectId;

    public FileNotFoundException(UUID relatedObjectId) {
        super(String.format("File with id = '%s' is not found", relatedObjectId));
        this.relatedObjectId = relatedObjectId;
    }
}
