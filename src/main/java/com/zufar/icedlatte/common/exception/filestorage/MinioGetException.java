package com.zufar.icedlatte.common.exception.filestorage;

import lombok.Getter;

import java.util.UUID;

@Getter
public class MinioGetException extends RuntimeException {

    private final String fileName;

    public MinioGetException(final String fileName) {
        super(String.format("The image with file name = %s is not found.", fileName));
        this.fileName = fileName;
    }
}
