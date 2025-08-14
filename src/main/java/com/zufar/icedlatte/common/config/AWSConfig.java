package com.zufar.icedlatte.common.config;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public AmazonS3 amazonS3() {
        try {
            String sessionToken = System.getenv("AWS_SESSION_TOKEN");
            BasicSessionCredentials awsCreds = new BasicSessionCredentials(accessKey, secretKey, sessionToken);
            return AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                    .withRegion(region)
                    .build();
        } catch (AmazonClientException ace) {
            log.error("AWS S3 Client Error: {}", ace.getMessage(), ace);
            throw new RuntimeException("Failed to create AmazonS3 client", ace);
        }
    }
}
