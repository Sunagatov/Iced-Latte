package com.zufar.icedlatte.filestorage.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

public interface ObjectStorage {

    boolean isConfigured();

    void upload(@NonNull MultipartFile file, @NonNull String bucketName, @NonNull String fileName);

    void uploadDirectory(@NonNull String bucketName, @NonNull String directoryPath) throws IOException;

    void delete(@NonNull FileMetadataDto fileMetadataDto);

    Optional<String> getUrl(@NonNull FileMetadataDto fileMetadataDto);

    List<String> listObjectKeys(@NonNull String bucketName);
}
