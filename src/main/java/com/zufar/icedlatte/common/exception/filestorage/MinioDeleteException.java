package com.zufar.icedlatte.common.exception.filestorage;

import lombok.Getter;

import java.util.UUID;

@Getter
public class MinioDeleteException extends RuntimeException {

    private final String fileName;

    public MinioDeleteException(final String fileName) {
        super(String.format("The image with file name = %s is not deleted.", fileName));
        this.fileName = fileName;
    }
}
