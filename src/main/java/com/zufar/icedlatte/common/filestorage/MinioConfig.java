package com.zufar.icedlatte.common.filestorage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${spring.minio.endpoint}")
    private String endpoint;
    @Value("${spring.minio.accessKey}")
    private String accessKey;
    @Value("${spring.minio.secretKey}")
    private String secretKey;
    @Value("${spring.minio.bucket}")
    private String avatarBucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint, 9000, false)
                .credentials(accessKey, secretKey)
                .build();
        createBuckets(minioClient);
        return minioClient;
    }

    @PostConstruct
    public void createBuckets(MinioClient minioClient) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(avatarBucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(avatarBucket).build());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
