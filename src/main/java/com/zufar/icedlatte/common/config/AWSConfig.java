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
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Slf4j
@Configuration
public class AWSConfig {

    private static final String AWS_SESSION_TOKEN = "AWS_SESSION_TOKEN";

    @Value("${spring.aws.access-key}")
    private String accessKey;

    @Value("${spring.aws.secret-key}")
    private String secretKey;

    @Value("${spring.aws.region}")
    private String region;

    @Value("${spring.aws.endpoint-url:}")
    private String endpointUrl;

    @Bean
    @ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
    public S3Client s3Client() {
        try {
            var builder = S3Client.builder()
                    .credentialsProvider(buildCredentials())
                    .region(Region.of(region));
            if (!StringUtils.hasText(endpointUrl)) {
                return builder.build();
            }
            return builder.endpointOverride(URI.create(endpointUrl))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        } catch (SdkClientException ace) {
            log.error("aws.s3.client.init_error: region={}, endpointOverrideConfigured={}, exceptionClass={}",
                    region, StringUtils.hasText(endpointUrl), ace.getClass().getSimpleName(), ace);
            throw ace;
        }
    }

    @Bean
    @ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .credentialsProvider(buildCredentials())
                .region(Region.of(region));
        if (StringUtils.hasText(endpointUrl)) {
            builder.endpointOverride(URI.create(endpointUrl));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
    public CloudFrontClient cloudFrontClient() {
        return CloudFrontClient.builder()
                .credentialsProvider(buildCredentials())
                .region(Region.AWS_GLOBAL)
                .build();
    }

    private StaticCredentialsProvider buildCredentials() {
        String sessionToken = System.getenv(AWS_SESSION_TOKEN);
        return StringUtils.hasText(sessionToken)
                ? StaticCredentialsProvider.create(AwsSessionCredentials.create(accessKey, secretKey, sessionToken))
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

}
