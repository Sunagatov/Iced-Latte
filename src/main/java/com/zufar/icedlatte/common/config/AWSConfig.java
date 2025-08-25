package com.zufar.icedlatte.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class AWSConfig {

    @Value("${spring.aws.access-key}")
    private String accessKey;

    @Value("${spring.aws.secret-key}")
    private String secretKey;

    @Value("${spring.aws.region}")
    private String region;

    @Bean
    @ConditionalOnProperty(name = "spring.aws.access-key", havingValue = "your-aws-access-key")
    public S3Client s3Client() {
        if ("your-aws-access-key".equals(accessKey) || "your-aws-secret-key".equals(secretKey)) {
            log.warn("AWS credentials are placeholder values. S3Client will be disabled.");
            throw new RuntimeException("AWS not configured");
        }

        try {
            String sessionToken = System.getenv("AWS_SESSION_TOKEN");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                AwsSessionCredentials awsCreds = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
                return S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .region(Region.of(region))
                        .build();
            } else {
                AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
                return S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .region(Region.of(region))
                        .build();
            }
        } catch (SdkClientException ace) {
            log.error("AWS S3 Client Error: {}. Application will continue without S3 functionality.", ace.getMessage());
            throw new RuntimeException("Failed to create S3Client", ace);
        }
    }
}
