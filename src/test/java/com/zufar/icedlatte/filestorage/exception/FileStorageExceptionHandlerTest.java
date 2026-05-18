package com.zufar.icedlatte.filestorage.exception;

import com.zufar.icedlatte.common.exception.handler.ProblemDetailFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageExceptionHandler unit tests")
class FileStorageExceptionHandlerTest {

    @Mock
    private ProblemDetailFactory problemDetailFactory;

    @InjectMocks
    private FileStorageExceptionHandler handler;

    @Test
    @DisplayName("handleFileReadException returns BAD_REQUEST")
    void handleFileReadException() {
        FileReadException ex = new FileReadException("file.txt", new RuntimeException("read failed"));
        ProblemDetail expected = ProblemDetail.forStatus(400);
        when(problemDetailFactory.build("file-read-failed", "File read failed",
                HttpStatus.BAD_REQUEST, ex.getMessage())).thenReturn(expected);

        ProblemDetail result = handler.handleFileReadException(ex);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleFileUploadException returns INTERNAL_SERVER_ERROR")
    void handleFileUploadException() {
        FileUploadException ex = new FileUploadException("file.txt", new RuntimeException("upload failed"));
        ProblemDetail expected = ProblemDetail.forStatus(500);
        when(problemDetailFactory.build("file-upload-failed", "File upload failed",
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage())).thenReturn(expected);

        ResponseEntity<ProblemDetail> result = handler.handleFileUploadException(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleFileUploadException returns SERVICE_UNAVAILABLE when cause is IllegalStateException")
    void handleFileUploadExceptionStorageUnavailable() {
        FileUploadException ex = new FileUploadException("file.txt", new IllegalStateException("storage down"));
        ProblemDetail expected = ProblemDetail.forStatus(503);
        when(problemDetailFactory.build("file-upload-failed", "File upload failed",
                HttpStatus.SERVICE_UNAVAILABLE, "File storage is not available.")).thenReturn(expected);

        ResponseEntity<ProblemDetail> result = handler.handleFileUploadException(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result.getBody()).isEqualTo(expected);
    }
}
