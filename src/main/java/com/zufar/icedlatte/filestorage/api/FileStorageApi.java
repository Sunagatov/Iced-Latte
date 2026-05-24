package com.zufar.icedlatte.filestorage.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

public interface FileStorageApi {

    boolean isEnabled();

    void store(MultipartFile file, FileMetadataDto fileMetadataDto);

    void storeDirectory(String bucketName, String directoryPath) throws IOException;

    Optional<String> findFileUrl(UUID relatedObjectId);

    Map<UUID, String> findFileUrls(List<UUID> relatedObjectIds);

    void deleteFile(UUID relatedObjectId);

    void refreshBucketIndex(String bucketName);
}
