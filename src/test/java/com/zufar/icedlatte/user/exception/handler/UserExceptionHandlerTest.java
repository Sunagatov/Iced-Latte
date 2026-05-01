package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserExceptionHandler Tests")
class UserExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;

    @InjectMocks
    private UserExceptionHandler userExceptionHandler;

    @Test
    @DisplayName("Should return ApiErrorResponse with UNAUTHORIZED status when UsernameNotFoundException is thrown")
    void shouldReturnApiErrorResponseWithUnauthorizedStatusWhenUsernameNotFoundExceptionIsThrown() {
        UsernameNotFoundException exception = new UsernameNotFoundException("Email cannot be empty");
        LocalDateTime currentDateTime = LocalDateTime.now();
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "User not found",
                HttpStatus.UNAUTHORIZED.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse("User not found", HttpStatus.UNAUTHORIZED)).thenReturn(expectedResponse);

        ApiErrorResponse actualResponse = userExceptionHandler.handleUsernameNotFoundException(exception);

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(apiErrorResponseCreator).buildResponse("User not found", HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should map invalid avatar file type to a sanitized BAD_REQUEST message")
    void shouldHandleInvalidAvatarFileTypeException() {
        InvalidAvatarFileTypeException exception = new InvalidAvatarFileTypeException(
                "image/gif",
                java.util.List.of("image/jpeg", "image/png", "image/webp")
        );
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "Invalid file type. Allowed types: JPEG, PNG, WebP",
                400,
                LocalDateTime.now()
        );
        when(apiErrorResponseCreator.buildResponse(
                "Invalid file type. Allowed types: JPEG, PNG, WebP",
                HttpStatus.BAD_REQUEST
        )).thenReturn(expectedResponse);

        ApiErrorResponse result = userExceptionHandler.handleInvalidAvatarFileTypeException(exception);

        assertThat(result).isEqualTo(expectedResponse);
        verify(apiErrorResponseCreator).buildResponse(
                "Invalid file type. Allowed types: JPEG, PNG, WebP",
                HttpStatus.BAD_REQUEST
        );
    }
}
