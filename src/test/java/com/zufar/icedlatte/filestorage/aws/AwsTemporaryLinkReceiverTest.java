package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsTemporaryLinkReceiver unit tests")
class AwsTemporaryLinkReceiverTest {

    @Mock private S3Presigner s3Presigner;

    @InjectMocks private AwsTemporaryLinkReceiver receiver;

    @Test
    @DisplayName("returns a direct public URL when publicUrlBase is configured")
    void returnsDirectPublicUrlWhenPublicUrlBaseIsConfigured() {
        ReflectionTestUtils.setField(receiver, "publicUrlBase", "https://cdn.example.com");
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "bucket-name", "images/avatar.jpg");

        String result = receiver.generatePresignedUrlAsString(metadata);

        assertThat(result).isEqualTo("https://cdn.example.com/images/avatar.jpg");
    }

    @Test
    @DisplayName("presigns the S3 object when no public base is configured")
    void presignsS3ObjectWhenNoPublicBaseIsConfigured() throws Exception {
        ReflectionTestUtils.setField(receiver, "publicUrlBase", "");
        ReflectionTestUtils.setField(receiver, "linkExpirationTime", "PT15M");
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "bucket-name", "images/avatar.jpg");
        PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(PresignedGetObjectRequest.class);

        when(s3Presigner.presignGetObject(org.mockito.ArgumentMatchers.<Consumer<GetObjectPresignRequest.Builder>>any()))
                .thenReturn(presigned);
        when(presigned.url()).thenReturn(URI.create("https://signed.example.com/images/avatar.jpg").toURL());

        String result = receiver.generatePresignedUrlAsString(metadata);

        assertThat(result).isEqualTo("https://signed.example.com/images/avatar.jpg");

        ArgumentCaptor<Consumer<GetObjectPresignRequest.Builder>> requestCaptor = consumerCaptor();
        verify(s3Presigner).presignGetObject(requestCaptor.capture());
        GetObjectPresignRequest.Builder builder = GetObjectPresignRequest.builder();
        requestCaptor.getValue().accept(builder);
        GetObjectPresignRequest request = builder.build();
        assertThat(request.signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(request.getObjectRequest().bucket()).isEqualTo("bucket-name");
        assertThat(request.getObjectRequest().key()).isEqualTo("images/avatar.jpg");
    }

    @Test
    @DisplayName("returns null when presigning fails")
    void returnsNullWhenPresigningFails() {
        ReflectionTestUtils.setField(receiver, "publicUrlBase", "");
        ReflectionTestUtils.setField(receiver, "linkExpirationTime", "PT15M");
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "bucket-name", "images/avatar.jpg");

        when(s3Presigner.presignGetObject(org.mockito.ArgumentMatchers.<Consumer<GetObjectPresignRequest.Builder>>any()))
                .thenThrow(SdkClientException.create("offline"));

        assertThat(receiver.generatePresignedUrlAsString(metadata)).isNull();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Consumer<GetObjectPresignRequest.Builder>> consumerCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Consumer.class);
    }
}
