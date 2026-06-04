package com.zufar.icedlatte.filestorage.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

public interface ObjectStorage {

    boolean isConfigured();

    void upload(MultipartFile file, String bucketName, String fileName);

    void uploadDirectory(String bucketName, String directoryPath) throws IOException;

    void delete(FileMetadataDto fileMetadataDto);

    Optional<String> getUrl(FileMetadataDto fileMetadataDto);

    List<String> listObjectKeys(String bucketName);
}
