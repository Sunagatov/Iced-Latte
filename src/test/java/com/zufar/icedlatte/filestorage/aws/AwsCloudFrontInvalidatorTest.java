package com.zufar.icedlatte.filestorage.aws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsCloudFrontInvalidator unit tests")
class AwsCloudFrontInvalidatorTest {

    @Mock private CloudFrontClient cloudFrontClient;

    @InjectMocks private AwsCloudFrontInvalidator invalidator;

    @Test
    @DisplayName("skips invalidation when the distribution ID is not configured")
    void skipsInvalidationWhenDistributionIdIsNotConfigured() {
        ReflectionTestUtils.setField(invalidator, "distributionId", " ");

        invalidator.invalidate("images/avatar.jpg");

        verifyNoInteractions(cloudFrontClient);
    }

    @Test
    @DisplayName("creates a one-path invalidation for the requested key")
    void createsOnePathInvalidationForRequestedKey() {
        ReflectionTestUtils.setField(invalidator, "distributionId", "distribution-123");

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Consumer<CreateInvalidationRequest.Builder>> consumerCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Consumer.class);
    }
}
