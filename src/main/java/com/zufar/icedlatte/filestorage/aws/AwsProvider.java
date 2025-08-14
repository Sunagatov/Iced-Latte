package com.zufar.icedlatte.filestorage.aws;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
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
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(bucketName);
            List<S3ObjectSummary> allObjects = new ArrayList<>();
            ListObjectsV2Result result;
            do {
                result = amazonS3.listObjectsV2(listObjectsV2Request);
                allObjects.addAll(result.getObjectSummaries());
                String token = result.getNextContinuationToken();
                listObjectsV2Request.setContinuationToken(token);
            } while (result.isTruncated());
            return getFileMetadataDtos(allObjects, bucketName);
        } catch (AmazonS3Exception e) {
            log.warn("Error accessing AWS S3 bucket", e);
            return List.of();
        } catch (SdkClientException e) {
            log.warn("AWS SDK client error", e);
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
