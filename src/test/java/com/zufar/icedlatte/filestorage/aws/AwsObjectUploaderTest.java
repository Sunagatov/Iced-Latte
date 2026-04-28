package com.zufar.icedlatte.filestorage.aws;

import com.zufar.icedlatte.filestorage.exception.FileReadException;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsObjectUploader unit tests")
class AwsObjectUploaderTest {

    @Mock private S3Client s3Client;

    @InjectMocks private AwsObjectUploader uploader;

    @Test
    @DisplayName("uploadFile sends bucket, key, content type, and length to S3")
    void uploadFileSendsBucketKeyContentTypeAndLengthToS3() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "hello".getBytes()
        );

        uploader.uploadFile(file, "bucket-name", "images/avatar.jpg");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("bucket-name");
        assertThat(request.key()).isEqualTo("images/avatar.jpg");
        assertThat(request.contentType()).isEqualTo("image/jpeg");
        assertThat(request.contentLength()).isEqualTo(5L);
    }

    @Test
    @DisplayName("uploadFile wraps unreadable multipart streams")
    void uploadFileWrapsUnreadableMultipartStreams() throws IOException {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("broken stream"));

        assertThatThrownBy(() -> uploader.uploadFile(file, "bucket-name", "images/avatar.jpg"))
                .isInstanceOf(FileReadException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("uploadFile wraps S3 service errors")
    void uploadFileWrapsS3ServiceErrors() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "hello".getBytes()
        );
        doThrow(S3Exception.builder().message("denied").build())
                .when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> uploader.uploadFile(file, "bucket-name", "images/avatar.jpg"))
                .isInstanceOf(FileUploadException.class)
                .hasCauseInstanceOf(S3Exception.class);
    }

    @Test
    @DisplayName("uploadFileDirectory uploads each regular file with a relative S3 key")
    void uploadFileDirectoryUploadsEachRegularFileWithRelativeS3Key(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("root.txt"), "root");
        Files.createDirectories(tempDir.resolve("nested"));
        Files.writeString(tempDir.resolve("nested/child.txt"), "child");

        uploader.uploadFileDirectory("bucket-name", tempDir.toString());

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, org.mockito.Mockito.times(2)).putObject(requestCaptor.capture(), any(RequestBody.class));

        List<String> keys = requestCaptor.getAllValues().stream().map(PutObjectRequest::key).sorted().toList();
        assertThat(keys).containsExactly("nested/child.txt", "root.txt");
        assertThat(requestCaptor.getAllValues()).allMatch(request -> "bucket-name".equals(request.bucket()));
    }

    @Test
    @DisplayName("uploadFileDirectory wraps AWS client connectivity failures")
    void uploadFileDirectoryWrapsAwsClientConnectivityFailures(@TempDir Path tempDir) throws IOException {
        Path file = Files.writeString(tempDir.resolve("root.txt"), "root");
        doThrow(SdkClientException.create("offline"))
                .when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> uploader.uploadFileDirectory("bucket-name", tempDir.toString()))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining(file.toString())
                .hasCauseInstanceOf(SdkClientException.class);
    }
}
