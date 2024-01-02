package com.zufar.icedlatte.filestorage.exception;

import lombok.Getter;

@Getter
public class FileReadException extends RuntimeException {

    private final String fileName;

    public FileReadException(String fileName) {
        super("Failed to read file: " + fileName);
        this.fileName = fileName;
    }
}
