package com.zufar.icedlatte.common.filestorage.api;

import com.zufar.icedlatte.common.filestorage.minio.MinioObjectDeleter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDeleter {

    private final MinioObjectDeleter minioObjectDeleter;
    private final MinioFileService minioFileService;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID relatedObjectId) {
        minioFileService.getFileMetadataDto(relatedObjectId)
                .ifPresent(fileMetadata -> {
                    minioObjectDeleter.deleteFile(fileMetadata);
                    minioFileService.deleteByRelatedObjectId(relatedObjectId);
                    log.info("File was deleted from the file storage and file metadata was deleted from the database as well");
                });
    }
}
