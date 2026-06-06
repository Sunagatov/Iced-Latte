package com.zufar.icedlatte.filestorage.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsCloudFrontInvalidator unit tests")
class AwsCloudFrontInvalidatorTest {

    @Mock
    private CloudFrontClient cloudFrontClient;

    @Test
    @DisplayName("skips invalidation when the distribution ID is not configured")
    void skipsInvalidationWhenDistributionIdIsNotConfigured() {
        AwsCloudFrontInvalidator invalidator = new AwsCloudFrontInvalidator(cloudFrontClient, properties(" "));

        invalidator.invalidate("images/avatar.jpg");

        verifyNoInteractions(cloudFrontClient);
    }

    @Test
    @DisplayName("creates a one-path invalidation for the requested key")
    void createsOnePathInvalidationForRequestedKey() {
        AwsCloudFrontInvalidator invalidator =
                new AwsCloudFrontInvalidator(cloudFrontClient, properties("distribution-123"));

        invalidator.invalidate("images/avatar.jpg");

        ArgumentCaptor<Consumer<CreateInvalidationRequest.Builder>> consumerCaptor = consumerCaptor();
        verify(cloudFrontClient).createInvalidation(consumerCaptor.capture());

        CreateInvalidationRequest.Builder builder = CreateInvalidationRequest.builder();
        consumerCaptor.getValue().accept(builder);
        CreateInvalidationRequest request = builder.build();

        assertThat(request.distributionId()).isEqualTo("distribution-123");
        assertThat(request.invalidationBatch().paths().quantity()).isEqualTo(1);
        assertThat(request.invalidationBatch().paths().items()).containsExactly("/images/avatar.jpg");
        assertThat(request.invalidationBatch().callerReference()).isNotBlank();
    }

    @Test
    @DisplayName("normalizes leading slashes before invalidating")
    void normalizesLeadingSlashesBeforeInvalidating() {
        AwsCloudFrontInvalidator invalidator =
                new AwsCloudFrontInvalidator(cloudFrontClient, properties("distribution-123"));

        invalidator.invalidate("/images/avatar.jpg");

        ArgumentCaptor<Consumer<CreateInvalidationRequest.Builder>> consumerCaptor = consumerCaptor();
        verify(cloudFrontClient).createInvalidation(consumerCaptor.capture());

        CreateInvalidationRequest.Builder builder = CreateInvalidationRequest.builder();
        consumerCaptor.getValue().accept(builder);

        assertThat(builder.build().invalidationBatch().paths().items()).containsExactly("/images/avatar.jpg");
    }

    @Test
    @DisplayName("skips invalidation when the file key is blank")
    void skipsInvalidationWhenFileKeyIsBlank() {
        AwsCloudFrontInvalidator invalidator =
                new AwsCloudFrontInvalidator(cloudFrontClient, properties("distribution-123"));

        invalidator.invalidate(" ");

        verifyNoInteractions(cloudFrontClient);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Consumer<CreateInvalidationRequest.Builder>> consumerCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Consumer.class);
    }

    private static AwsProperties properties(String distributionId) {
        return new AwsProperties(
                "access-key",
                "secret-key",
                "eu-west-2",
                "",
                "",
                distributionId,
                Duration.ofHours(1),
                Duration.ofSeconds(10),
                Duration.ofSeconds(10),
                Duration.ofSeconds(60),
                Duration.ofSeconds(15));
    }
}
