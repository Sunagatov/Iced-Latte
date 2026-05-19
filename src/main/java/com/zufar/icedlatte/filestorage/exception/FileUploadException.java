package com.zufar.icedlatte.filestorage.exception;

public final class FileUploadException extends FileStorageException {

    public FileUploadException(String fileName, Throwable cause) {
        super("Failed to upload file: " + fileName, fileName, cause);
    }
}
