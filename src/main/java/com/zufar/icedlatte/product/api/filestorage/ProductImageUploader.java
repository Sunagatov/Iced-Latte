package com.zufar.icedlatte.product.api.filestorage;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.zufar.icedlatte.common.filestorage.api.MinioFileService;
import com.zufar.icedlatte.common.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageUploader {

    @Value("${spring.minio.buckets.product-picture")
    private static String bucketName;

    private final AmazonS3 amazonS3;
    private final MinioFileService minioFileService;

    @Transactional
    public void uploadProductImages() {
        try {
            ObjectListing objectListing = amazonS3.listObjects(bucketName);
            List<FileMetadataDto> fileMetadataDtos = getFileMetadataDtos(objectListing.getObjectSummaries());
            minioFileService.saveAll(fileMetadataDtos);
        } catch (Exception e) {
            log.warn("File upload error", e);
        }
    }

    private List<FileMetadataDto> getFileMetadataDtos(List<S3ObjectSummary> objectSummaries) {
        List<FileMetadataDto> fileMetadataDtos = new ArrayList<>();

        objectSummaries.forEach(objectSummary -> {
            String[] parts = objectSummary.getKey().split("/");
            String relatedObjectId = parts[0];
            String fileName = parts[1];
            FileMetadataDto fileMetadataDto = new FileMetadataDto(
                    UUID.fromString(relatedObjectId),
                    bucketName,
                    fileName
            );
            fileMetadataDtos.add(fileMetadataDto);
        });

        return fileMetadataDtos;
    }
}
