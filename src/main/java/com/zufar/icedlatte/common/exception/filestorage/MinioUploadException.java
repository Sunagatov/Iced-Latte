package com.zufar.icedlatte.common.exception.filestorage;

import lombok.Getter;

import java.util.UUID;

@Getter
public class MinioUploadException extends RuntimeException {

    private final String fileName;

    public MinioUploadException(final String fileName) {
        super(String.format("The image with file name = %s is not uploaded.", fileName));
        this.fileName = fileName;
    }
}
