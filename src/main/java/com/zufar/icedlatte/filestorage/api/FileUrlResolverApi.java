package com.zufar.icedlatte.filestorage.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

public interface FileUrlResolverApi {

    Optional<String> findFileUrl(UUID relatedObjectId);

    Optional<String> findFileUrl(FileMetadataDto fileMetadataDto);

    Map<UUID, String> findFileUrls(List<UUID> relatedObjectIds);
}
