package com.zufar.icedlatte.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Slf4j
@Configuration
public class AWSConfig {

    @Value("${spring.aws.access-key}")
    private String accessKey;

    @Value("${spring.aws.secret-key}")
    private String secretKey;

    @Value("${spring.aws.region}")
    private String region;

    @Value("${spring.aws.endpoint-url:}")
    private String endpointUrl;

    @Bean
    @ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
    public S3Client s3Client() {
        try {
            AwsBasicCredentials awsCreds;
            String sessionToken = System.getenv("AWS_SESSION_TOKEN");
            if (StringUtils.hasText(sessionToken)) {
                AwsSessionCredentials sessionCreds = AwsSessionCredentials.create(accessKey, secretKey, sessionToken);
                var builder = S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(sessionCreds))
                        .region(Region.of(region));
                applyEndpointOverride(builder);
                return builder.build();
            } else {
                awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
                var builder = S3Client.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                        .region(Region.of(region));
                applyEndpointOverride(builder);
                return builder.build();
            }
        } catch (SdkClientException ace) {
            log.error("S3 Client Error: {}. Application will continue without S3 functionality.", ace.getMessage());
            throw new RuntimeException("Failed to create S3Client", ace);
        }
    }

    @Bean
    @ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
    public S3Presigner s3Presigner() {
        String sessionToken = System.getenv("AWS_SESSION_TOKEN");
        S3Presigner.Builder builder;
        if (StringUtils.hasText(sessionToken)) {
            builder = S3Presigner.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsSessionCredentials.create(accessKey, secretKey, sessionToken)))
                    .region(Region.of(region));
        } else {
            builder = S3Presigner.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .region(Region.of(region));
        }
        if (StringUtils.hasText(endpointUrl)) {
            builder.endpointOverride(URI.create(endpointUrl));
        }
        return builder.build();
    }

    private void applyEndpointOverride(S3ClientBuilder builder) {
        if (StringUtils.hasText(endpointUrl)) {
            builder.endpointOverride(URI.create(endpointUrl))
                   .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
    }
}
