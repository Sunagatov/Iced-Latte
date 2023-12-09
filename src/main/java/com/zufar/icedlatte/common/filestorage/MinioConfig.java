package com.zufar.icedlatte.common.filestorage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    private static final int MINIO_PORT = 9000;

    @Value("${spring.minio.url}")
    private String minioUrl;

    @Value("${spring.minio.secure}")
    private boolean minioSecure;

    @Value("${spring.minio.access-key}")
    private String minioAccessKey;

    @Value("${spring.minio.secret-key}")
    private String minioSecretKey;

    @Value("${spring.minio.buckets.user-avatar}")
    private String minioAvatarBucket;

    @Bean
    public MinioClient minioClient() throws Exception {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioUrl, MINIO_PORT, minioSecure)
                .credentials(minioAccessKey, minioSecretKey)
                .build();

        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioAvatarBucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioAvatarBucket).build());
        }

        return minioClient;
    }
}
