package com.zufar.icedlatte.filestorage.aws;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

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
            assertThat(client.serviceClientConfiguration()
                            .overrideConfiguration()
                            .apiCallTimeout())
                    .contains(Duration.ofSeconds(60));
            assertThat(client.serviceClientConfiguration()
                            .overrideConfiguration()
                            .apiCallAttemptTimeout())
                    .contains(Duration.ofSeconds(15));
        }
    }

    @Test
    @DisplayName("s3Client omits endpoint override when none is configured")
    void s3ClientOmitsEndpointOverrideWhenNoneConfigured() {
        AWSConfig config = configured("");

        try (S3Client client = config.s3Client()) {
            assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.of("eu-west-2"));
            String envEndpoint = System.getenv("AWS_ENDPOINT_URL");
            if (envEndpoint == null || envEndpoint.isBlank()) {
                assertThat(client.serviceClientConfiguration().endpointOverride())
                        .isEmpty();
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
            assertThat(client.serviceClientConfiguration()
                            .overrideConfiguration()
                            .apiCallTimeout())
                    .contains(Duration.ofSeconds(60));
            assertThat(client.serviceClientConfiguration()
                            .overrideConfiguration()
                            .apiCallAttemptTimeout())
                    .contains(Duration.ofSeconds(15));
        }
    }

    private static AWSConfig configured(String endpointUrl) {
        return new AWSConfig(new AwsProperties(
                "access-key",
                "secret-key",
                "eu-west-2",
                endpointUrl,
                "",
                "",
                Duration.ofHours(1),
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(60),
                Duration.ofSeconds(15)));
    }
}
