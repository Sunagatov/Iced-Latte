package com.zufar.icedlatte.filestorage.exception;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommonExceptionHandler unit tests")
class CommonExceptionHandlerTest {

    @Mock private ApiErrorResponseCreator apiErrorResponseCreator;
    @InjectMocks private CommonExceptionHandler handler;

    @Test
    @DisplayName("handleFileReadException returns BAD_REQUEST")
    void handleFileReadException() {
        FileReadException ex = new FileReadException("file.txt", new RuntimeException("read failed"));
        ApiErrorResponse expected = new ApiErrorResponse("read failed", 400, LocalDateTime.now());
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);
        assertThat(handler.handleFileReadException(ex)).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleFileUploadException returns INTERNAL_SERVER_ERROR")
    void handleFileUploadException() {
        FileUploadException ex = new FileUploadException("file.txt", new RuntimeException("upload failed"));
        ApiErrorResponse expected = new ApiErrorResponse("upload failed", 500, LocalDateTime.now());
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR)).thenReturn(expected);
        ResponseEntity<ApiErrorResponse> result = handler.handleFileUploadException(ex);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleFileUploadException returns SERVICE_UNAVAILABLE when cause is IllegalStateException")
    void handleFileUploadExceptionStorageUnavailable() {
        FileUploadException ex = new FileUploadException("file.txt", new IllegalStateException("storage down"));
        ApiErrorResponse expected = new ApiErrorResponse("File storage is not available", 503, LocalDateTime.now());
        when(apiErrorResponseCreator.buildResponse("File storage is not available", HttpStatus.SERVICE_UNAVAILABLE)).thenReturn(expected);
        ResponseEntity<ApiErrorResponse> result = handler.handleFileUploadException(ex);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result.getBody()).isEqualTo(expected);
    }
}
