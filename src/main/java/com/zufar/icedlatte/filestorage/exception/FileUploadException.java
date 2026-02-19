package com.zufar.icedlatte.filestorage.exception;

import lombok.Getter;

@Getter
public class FileUploadException extends RuntimeException {

    private final String fileName;

    public FileUploadException(String fileName, Throwable cause) {
        super("Failed to upload file: " + fileName, cause);
        this.fileName = fileName;
    }
}
