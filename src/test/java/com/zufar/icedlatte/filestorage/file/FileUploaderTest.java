package com.zufar.icedlatte.filestorage.file;

import com.zufar.icedlatte.filestorage.aws.AwsObjectUploader;
import com.zufar.icedlatte.filestorage.exception.FileUploadException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("FileUploader")
class FileUploaderTest {

    private final AwsObjectUploader awsObjectUploader = mock(AwsObjectUploader.class);
    private final MultipartFile multipartFile = mock(MultipartFile.class);

    @Test
    @DisplayName("uploads file when AWS is configured")
    void uploadsFileWhenAwsConfigured() {
        FileUploader uploader = new FileUploader(awsObjectUploader);

        boolean result = uploader.upload(multipartFile, "products", "coffee.png");

        assertThat(result).isTrue();
        verify(awsObjectUploader).uploadFile(multipartFile, "products", "coffee.png");
    }

    @Test
    @DisplayName("returns false when AWS is disabled")
    void returnsFalseWhenAwsDisabled() {
        FileUploader uploader = new FileUploader(null);

        boolean result = uploader.upload(multipartFile, "products", "coffee.png");

        assertThat(result).isFalse();
        verifyNoInteractions(multipartFile);
    }

    @Test
    @DisplayName("uploads directory when AWS is configured")
    void uploadsDirectoryWhenAwsConfigured() throws IOException {
        FileUploader uploader = new FileUploader(awsObjectUploader);

        uploader.uploadDirectory("products", "assets/images");

        verify(awsObjectUploader).uploadFileDirectory("products", "assets/images");
    }

    @Test
    @DisplayName("wraps directory upload IO failures")
    void wrapsDirectoryUploadIoFailures() throws IOException {
        FileUploader uploader = new FileUploader(awsObjectUploader);
        doThrow(new IOException("disk error"))
                .when(awsObjectUploader).uploadFileDirectory("products", "assets/images");

        assertThatThrownBy(() -> uploader.uploadDirectory("products", "assets/images"))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("assets/images");
    }

    @Test
    @DisplayName("skips directory upload when AWS is disabled")
    void skipsDirectoryUploadWhenAwsDisabled() {
        FileUploader uploader = new FileUploader(null);

        uploader.uploadDirectory("products", "assets/images");
    }
}
