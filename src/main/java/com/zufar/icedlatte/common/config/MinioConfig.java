package com.zufar.icedlatte.common.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class MinioConfig {

    @Value("${spring.minio.url}")
    private String minioUrl;

    @Value("${spring.minio.access-key}")
    private String minioAccessKey;

    @Value("${spring.minio.secret-key}")
    private String minioSecretKey;

    @Value("${spring.minio.buckets.user-avatar}")
    private String minioAvatarBucket;

    @Value("${spring.minio.buckets.product-picture")
    private static String productPictureBucket;

    @Value("${spring.minio.region}")
    private String region;

    @Bean
    public AmazonS3 amazonS3() {
        AmazonS3 amazonS3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(minioUrl, region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(minioAccessKey, minioSecretKey)))
                .withPathStyleAccessEnabled(true)
                .build();
        try {
            createBucket(amazonS3, minioAvatarBucket);
            createBucket(amazonS3, productPictureBucket);
        } catch (Exception exception) {
            log.error("Creating AmazonS3 bucket was failed", exception);
        }

        return amazonS3;
    }

    private void createBucket(AmazonS3 amazonS3, String bucketName) {
        if (!amazonS3.doesBucketExistV2(bucketName)) {
            amazonS3.createBucket(bucketName);
        }
    }
}
