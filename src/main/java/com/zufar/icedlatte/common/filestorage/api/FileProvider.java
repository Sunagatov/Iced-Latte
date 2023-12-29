package com.zufar.icedlatte.common.filestorage.api;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.zufar.icedlatte.common.filestorage.dto.FileMetadataDto;
import com.zufar.icedlatte.common.filestorage.minio.MinioTemporaryLinkReceiver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProvider {

    private final MinioTemporaryLinkReceiver minioTemporaryLinkReceiver;
    private final MinioFileService minioFileService;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Optional<String> getRelatedObjectUrl(final UUID relatedObjectId) {
        return minioFileService.getFileMetadataDto(relatedObjectId)
                .map(minioTemporaryLinkReceiver::generatePresignedUrlAsString);
    }

    private final AmazonS3 amazonS3;
}
