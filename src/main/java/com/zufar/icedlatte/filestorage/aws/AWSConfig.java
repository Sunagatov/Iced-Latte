package com.zufar.icedlatte.filestorage.aws;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AWSConfig {

    private final AwsProperties awsProperties;

    @Bean
    @ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
    public S3Client s3Client() {
        String endpointUrl = awsProperties.endpointUrl();
        String region = awsProperties.region();

        try {
            var builder = S3Client.builder()
                    .credentialsProvider(buildCredentials())
                    .region(Region.of(region))
                    .httpClientBuilder(buildHttpClient())
                    .overrideConfiguration(buildOverrideConfiguration());
            if (!StringUtils.hasText(endpointUrl)) {
                return builder.build();
            }
            return builder.endpointOverride(URI.create(endpointUrl))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        } catch (SdkClientException ace) {
            String logMessage = "aws.s3.client.init_error: region={}, endpointOverrideConfigured={}, exceptionClass={}";
            log.error(
                    logMessage,
                    region,
                    StringUtils.hasText(endpointUrl),
                    ace.getClass().getSimpleName(),
                    ace);
            throw ace;
        }
    }

    @Bean
    @ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder =
                S3Presigner.builder().credentialsProvider(buildCredentials()).region(Region.of(awsProperties.region()));
        if (StringUtils.hasText(awsProperties.endpointUrl())) {
            builder.endpointOverride(URI.create(awsProperties.endpointUrl()));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.aws.enabled", havingValue = "true")
    public CloudFrontClient cloudFrontClient() {
        return CloudFrontClient.builder()
                .credentialsProvider(buildCredentials())
                .region(Region.AWS_GLOBAL)
                .httpClientBuilder(buildHttpClient())
                .overrideConfiguration(buildOverrideConfiguration())
                .build();
    }

    private UrlConnectionHttpClient.Builder buildHttpClient() {
        return UrlConnectionHttpClient.builder()
                .connectionTimeout(awsProperties.connectTimeout())
                .socketTimeout(awsProperties.readTimeout());
    }

    private ClientOverrideConfiguration buildOverrideConfiguration() {
        return ClientOverrideConfiguration.builder()
                .apiCallAttemptTimeout(awsProperties.apiCallAttemptTimeout())
                .apiCallTimeout(awsProperties.apiCallTimeout())
                .build();
    }

    private StaticCredentialsProvider buildCredentials() {
        String sessionToken = System.getenv("AWS_SESSION_TOKEN");
        String accessKey = awsProperties.accessKey();
        String secretKey = awsProperties.secretKey();

        return StringUtils.hasText(sessionToken)
                ? StaticCredentialsProvider.create(AwsSessionCredentials.create(accessKey, secretKey, sessionToken))
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
