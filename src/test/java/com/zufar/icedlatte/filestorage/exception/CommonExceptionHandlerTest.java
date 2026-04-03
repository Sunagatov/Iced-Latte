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
        assertThat(handler.handleFileUploadException(ex)).isEqualTo(expected);
    }
}
