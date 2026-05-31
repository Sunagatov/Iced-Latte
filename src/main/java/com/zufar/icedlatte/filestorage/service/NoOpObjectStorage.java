package com.zufar.icedlatte.filestorage.service;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;
import com.zufar.icedlatte.filestorage.exception.FileListException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnMissingBean(ObjectStorage.class)
public class NoOpObjectStorage implements ObjectStorage {

    public NoOpObjectStorage() {
        log.info("storage.aws.disabled: object storage operations will be skipped");
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public void upload(MultipartFile file, String bucketName, String fileName) {
        throw new FileUploadException(fileName, new IllegalStateException("File storage is not configured"));
    }

    @Override
    public void uploadDirectory(String bucketName, String directoryPath) {
        throw new FileUploadException(directoryPath, new IllegalStateException("File storage is not configured"));
    }

    @Override
    public void delete(FileMetadataDto fileMetadataDto) {
        log.debug(
                "file.delete.skipped: reason=aws_not_configured, bucket={}, key={}",
                fileMetadataDto.bucketName(),
                fileMetadataDto.fileName());
    }

    @Override
    public Optional<String> getUrl(FileMetadataDto fileMetadataDto) {
        log.debug(
                "file.url.skipped: reason=aws_not_configured, bucket={}, key={}",
                fileMetadataDto.bucketName(),
                fileMetadataDto.fileName());
        return Optional.empty();
    }

    @Override
    public List<String> listObjectKeys(String bucketName) {
        throw new FileListException(bucketName, new IllegalStateException("File storage is not configured"));
    }
}
