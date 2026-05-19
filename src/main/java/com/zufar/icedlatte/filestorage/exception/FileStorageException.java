package com.zufar.icedlatte.filestorage.exception;

import lombok.Getter;

/**
 * Sealed base for all file-storage exceptions.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
@Getter
public sealed class FileStorageException extends RuntimeException
        permits FileReadException, FileUploadException {

    private final String fileName;

    protected FileStorageException(String message, String fileName, Throwable cause) {
        super(message, cause);
        this.fileName = fileName;
    }
}
