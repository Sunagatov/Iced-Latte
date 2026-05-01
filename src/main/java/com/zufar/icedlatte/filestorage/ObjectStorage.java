package com.zufar.icedlatte.filestorage;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ObjectStorage {

    boolean isConfigured();

    void upload(MultipartFile file, String bucketName, String fileName);

    void uploadDirectory(String bucketName, String directoryPath) throws IOException;

    void delete(FileMetadataDto fileMetadataDto);

    Optional<String> getUrl(FileMetadataDto fileMetadataDto);

    List<String> listObjectKeys(String bucketName);
}
