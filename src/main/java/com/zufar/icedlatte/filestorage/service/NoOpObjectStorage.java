package com.zufar.icedlatte.filestorage.service;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.zufar.icedlatte.filestorage.api.dto.FileMetadataDto;

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
    public void upload(@NonNull MultipartFile file, @NonNull String bucketName, @NonNull String fileName) {
        log.debug("file.upload.skipped: reason=aws_not_configured, bucket={}, key={}", bucketName, fileName);
    }

    @Override
    public void uploadDirectory(@NonNull String bucketName, @NonNull String directoryPath) {
        log.debug("file.dir_upload.skipped: reason=aws_not_configured, bucket={}, path={}", bucketName, directoryPath);
    }

    @Override
    public void delete(@NonNull FileMetadataDto fileMetadataDto) {
        log.debug(
                "file.delete.skipped: reason=aws_not_configured, bucket={}, key={}",
                fileMetadataDto.bucketName(),
                fileMetadataDto.fileName());
    }

    @Override
    public @NonNull Optional<String> getUrl(@NonNull FileMetadataDto fileMetadataDto) {
        log.debug(
                "file.url.skipped: reason=aws_not_configured, bucket={}, key={}",
                fileMetadataDto.bucketName(),
                fileMetadataDto.fileName());
        return Optional.empty();
    }

    @Override
    public @NonNull List<String> listObjectKeys(@NonNull String bucketName) {
        log.debug("file.list.skipped: reason=aws_not_configured, bucket={}", bucketName);
        return List.of();
    }
}
