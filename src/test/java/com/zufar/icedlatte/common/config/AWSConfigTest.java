package com.zufar.icedlatte.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AWSConfig unit tests")
class AWSConfigTest {

    @Test
    @DisplayName("s3Client uses configured region and optional endpoint override")
    void s3ClientUsesConfiguredRegionAndOptionalEndpointOverride() {
        AWSConfig config = configured("http://localhost:9000");

        try (S3Client client = config.s3Client()) {
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.of("eu-west-2"));
            assertThat(client.serviceClientConfiguration().endpointOverride())
                    .isEqualTo(Optional.of(URI.create("http://localhost:9000")));
        }
    }

    @Test
    @DisplayName("s3Client omits endpoint override when none is configured")
    void s3ClientOmitsEndpointOverrideWhenNoneConfigured() {
        AWSConfig config = configured("");

        try (S3Client client = config.s3Client()) {
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.of("eu-west-2"));
            // When AWS_ENDPOINT_URL env var is set (e.g., local Docker MinIO),
            // the SDK picks it up automatically. Only assert empty when env is unset.
            String envEndpoint = System.getenv("AWS_ENDPOINT_URL");
            if (envEndpoint == null || envEndpoint.isBlank()) {
                assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("s3Presigner uses configured region and endpoint override")
    void s3PresignerUsesConfiguredRegionAndEndpointOverride() {
        AWSConfig config = configured("http://localhost:9000");

        try (S3Presigner presigner = config.s3Presigner()) {
            Object region = ReflectionTestUtils.invokeMethod(presigner, "region");
            Object endpointOverride = ReflectionTestUtils.invokeMethod(presigner, "endpointOverride");

            assertThat(region).isEqualTo(Region.of("eu-west-2"));
            assertThat(endpointOverride).isEqualTo(URI.create("http://localhost:9000"));
        }
    }

    @Test
    @DisplayName("cloudFrontClient uses the global region")
    void cloudFrontClientUsesGlobalRegion() {
        AWSConfig config = configured("");

        try (CloudFrontClient client = config.cloudFrontClient()) {
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.AWS_GLOBAL);
        }
    }

    private static AWSConfig configured(String endpointUrl) {
        AWSConfig config = new AWSConfig();
        ReflectionTestUtils.setField(config, "accessKey", "access-key");
        ReflectionTestUtils.setField(config, "secretKey", "secret-key");
        ReflectionTestUtils.setField(config, "region", "eu-west-2");
        ReflectionTestUtils.setField(config, "endpointUrl", endpointUrl);
        return config;
    }
}
