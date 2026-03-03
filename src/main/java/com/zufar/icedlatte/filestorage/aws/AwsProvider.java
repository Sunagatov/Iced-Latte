package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class AwsProvider {

    private final S3Client s3Client;

    public List<FileMetadataDto> getProductImagesFromAWS(String bucketName) {
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();
            List<S3Object> allObjects = s3Client.listObjectsV2Paginator(request)
                    .contents()
                    .stream()
                    .toList();
            return getFileMetadataDtos(allObjects, bucketName);
        } catch (S3Exception e) {
            log.error("aws.s3.list.error: message={}", e.getMessage(), e);
            return List.of();
        } catch (SdkClientException e) {
            log.error("aws.s3.list.unreachable: message={}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<FileMetadataDto> getFileMetadataDtos(List<S3Object> objects, String bucketName) {
        List<FileMetadataDto> fileMetadataDtos = new ArrayList<>();

        objects.forEach(s3Object -> {
            String fileName = s3Object.key();
            String[] parts = fileName.split("/");
            String[] packageName = parts[0].split("_");
            if (packageName.length < 2) {
                log.warn("aws.s3.key.skipped: key={}", fileName);
                return;
            }
            try {
                fileMetadataDtos.add(new FileMetadataDto(UUID.fromString(packageName[1]), bucketName, fileName));
            } catch (IllegalArgumentException e) {
                log.warn("aws.s3.key.invalid_uuid: key={}", fileName);
            }
        });

        return fileMetadataDtos;
    }
}
