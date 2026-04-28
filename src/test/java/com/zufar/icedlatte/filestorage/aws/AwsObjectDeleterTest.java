package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.dto.FileMetadataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsObjectDeleter unit tests")
class AwsObjectDeleterTest {

    @Mock private S3Client s3Client;

    @InjectMocks private AwsObjectDeleter deleter;

    @Test
    @DisplayName("deleteFile sends the expected bucket and key to S3")
    void deleteFileSendsExpectedBucketAndKeyToS3() {
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "bucket-name", "images/avatar.jpg");

        deleter.deleteFile(metadata);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("bucket-name");
        assertThat(requestCaptor.getValue().key()).isEqualTo("images/avatar.jpg");
    }

    @Test
    @DisplayName("deleteFile rethrows S3 service failures")
    void deleteFileRethrowsS3ServiceFailures() {
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "bucket-name", "images/avatar.jpg");
        S3Exception failure = (S3Exception) S3Exception.builder().message("denied").build();
        doThrow(failure).when(s3Client).deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class));

        assertThatThrownBy(() -> deleter.deleteFile(metadata)).isSameAs(failure);
    }

    @Test
    @DisplayName("deleteFile rethrows AWS client connectivity failures")
    void deleteFileRethrowsAwsClientConnectivityFailures() {
        FileMetadataDto metadata = new FileMetadataDto(UUID.randomUUID(), "bucket-name", "images/avatar.jpg");
        SdkClientException failure = SdkClientException.create("offline");
        doThrow(failure).when(s3Client).deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class));

        assertThatThrownBy(() -> deleter.deleteFile(metadata)).isSameAs(failure);
    }
}
