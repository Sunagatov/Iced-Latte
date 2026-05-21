package com.zufar.icedlatte.filestorage.api.dto;

import java.util.UUID;

public record FileMetadataDto(UUID relatedObjectId,
                              String bucketName,
                              String fileName) { }
