package com.zufar.icedlatte.common.filestorage;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MinioBucketCreator {

    private final MinioClient minioClient;

    public void create(String bucketName) {
        try {
            MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder().bucket(bucketName).build();
            minioClient.makeBucket(makeBucketArgs);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing storage", e);
        }
    }
}
