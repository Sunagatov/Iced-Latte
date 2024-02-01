package com.zufar.icedlatte.filestorage.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public record FileMetadataDto(UUID relatedObjectId,
                              String bucketName,
                              String fileName) { }
