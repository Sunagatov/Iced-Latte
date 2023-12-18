package com.zufar.icedlatte.common.filestorage.api;

import com.zufar.icedlatte.common.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.common.filestorage.minio.MinioObjectDeleter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileDeleter {

    private final MinioObjectDeleter minioObjectDeleter;
    private final MinioFileService minioFileService;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID relatedObjectId) {
        FileMetadataDto fileMetadata = minioFileService.getFileMetadataDto(relatedObjectId);
        minioObjectDeleter.deleteFile(fileMetadata);
        minioFileService.deleteByRelatedObjectId(relatedObjectId);
    }
}
