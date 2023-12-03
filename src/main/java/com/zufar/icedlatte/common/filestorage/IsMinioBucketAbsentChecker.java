package com.zufar.icedlatte.common.filestorage;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IsMinioBucketAbsentChecker {

    private final MinioClient minioClient;

    public boolean isAbsent(String bucketName) {
        try {
            BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder().bucket(bucketName).build();
            return !minioClient.bucketExists(bucketExistsArgs);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing storage", e);
        }
    }
}
