package com.zufar.icedlatte.filestorage.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AwsProvider {

    private final AmazonS3 amazonS3;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public List<FileMetadataDto> getProductImagesFromAWS(String bucketName) {
        try {
            ObjectListing objectListing = amazonS3.listObjects(bucketName);
            return getFileMetadataDtos(objectListing.getObjectSummaries(), bucketName);
        } catch (Exception e) {
            log.warn("Product's files upload error", e);
            return List.of();
        }
    }

    private List<FileMetadataDto> getFileMetadataDtos(List<S3ObjectSummary> objectSummaries, String bucketName) {
        List<FileMetadataDto> fileMetadataDtos = new ArrayList<>();

        objectSummaries.forEach(objectSummary -> {
            String fileName = objectSummary.getKey();
            String[] parts = fileName.split("/");
            String[] packageName = parts[0].split("_");
            String relatedObjectId = packageName[1];
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
