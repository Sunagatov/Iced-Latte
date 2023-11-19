package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.handler.ErrorDebugMessageCreator;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserExceptionHandler Tests")
class UserExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;

    @Mock
    private ErrorDebugMessageCreator errorDebugMessageCreator;

    @InjectMocks
    private UserExceptionHandler userExceptionHandler;

    @Test
    @DisplayName("Should return ApiErrorResponse with NOT_FOUND status when UserNotFoundException is thrown")
    void shouldReturnApiErrorResponseWithNotFoundStatusWhenUserNotFoundExceptionThrown() {
        UUID userId = UUID.randomUUID();
        LocalDateTime currentDateTime = LocalDateTime.now();
        UserNotFoundException exception = new UserNotFoundException(userId);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "User with id = " + userId + " is not found.",
                HttpStatus.NOT_FOUND.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND)).thenReturn(expectedResponse);
        when(errorDebugMessageCreator.buildErrorDebugMessage(exception)).thenReturn("Error Debug Message");

        ApiErrorResponse actualResponse = userExceptionHandler.handleUserNotFoundException(exception);

        assertEquals(expectedResponse.httpStatusCode(), actualResponse.httpStatusCode());
        assertEquals(expectedResponse.message(), actualResponse.message());
        assertEquals(expectedResponse.timestamp(), actualResponse.timestamp());

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.NOT_FOUND);
        verify(errorDebugMessageCreator).buildErrorDebugMessage(exception);
    }
}
