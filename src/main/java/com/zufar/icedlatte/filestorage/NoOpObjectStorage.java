package com.zufar.icedlatte.filestorage;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

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
        log.debug("file.upload.skipped: reason=aws_not_configured, bucket={}, key={}", bucketName, fileName);
    }

    @Override
    public void uploadDirectory(String bucketName, String directoryPath) {
        log.debug("file.dir_upload.skipped: reason=aws_not_configured, bucket={}, path={}", bucketName, directoryPath);
    }

    @Override
    public void delete(FileMetadataDto fileMetadataDto) {
        log.debug("file.delete.skipped: reason=aws_not_configured, bucket={}, key={}",
                fileMetadataDto.bucketName(), fileMetadataDto.fileName());
    }

    @Override
    public Optional<String> getUrl(FileMetadataDto fileMetadataDto) {
        log.debug("file.url.skipped: reason=aws_not_configured, bucket={}, key={}",
                fileMetadataDto.bucketName(), fileMetadataDto.fileName());
        return Optional.empty();
    }

    @Override
    public List<String> listObjectKeys(String bucketName) {
        log.debug("file.list.skipped: reason=aws_not_configured, bucket={}", bucketName);
        return List.of();
    }
}
