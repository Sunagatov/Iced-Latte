package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.user.exception.DeliveryAddressNotFoundException;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.InvalidOldPasswordException;
import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;

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

        ApiErrorResponse actualResponse = userExceptionHandler.handleUserNotFoundException(exception);

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.NOT_FOUND);
    }

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
    @DisplayName("Should return ApiErrorResponse with UNAUTHORIZED status when InvalidOldPasswordException is thrown")
    void shouldReturnApiErrorResponseWithUnauthorizedStatusWhenInvalidOldPasswordExceptionIsThrown() {
        InvalidOldPasswordException exception = new InvalidOldPasswordException();
        LocalDateTime currentDateTime = LocalDateTime.now();
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                exception.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                currentDateTime
        );

        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED)).thenReturn(expectedResponse);

        ApiErrorResponse actualResponse = userExceptionHandler.handleInvalidOldPasswordException(exception);

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with NOT_FOUND when DeliveryAddressNotFoundException is thrown")
    void shouldHandleDeliveryAddressNotFoundException() {
        UUID addressId = UUID.randomUUID();
        DeliveryAddressNotFoundException exception = new DeliveryAddressNotFoundException(addressId);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(exception.getMessage(), 404, LocalDateTime.now());
        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND)).thenReturn(expectedResponse);

        ApiErrorResponse result = userExceptionHandler.handleDeliveryAddressNotFoundException(exception);

        assertThat(result).isEqualTo(expectedResponse);
        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return ApiErrorResponse with BAD_REQUEST when PutUsersBadRequestException is thrown")
    void shouldHandlePutUsersBadRequestException() {
        PutUsersBadRequestException exception = new PutUsersBadRequestException("invalid field");
        ApiErrorResponse expectedResponse = new ApiErrorResponse(exception.getMessage(), 400, LocalDateTime.now());
        when(apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST)).thenReturn(expectedResponse);

        ApiErrorResponse result = userExceptionHandler.handlePutUsersBadRequestException(exception);

        assertThat(result).isEqualTo(expectedResponse);
        verify(apiErrorResponseCreator).buildResponse(exception, HttpStatus.BAD_REQUEST);
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

    @Test
    @DisplayName("Should map data integrity violations to a sanitized BAD_REQUEST message")
    void shouldHandleDataIntegrityViolationException() {
        org.springframework.dao.DataIntegrityViolationException exception =
                new org.springframework.dao.DataIntegrityViolationException("duplicate key");
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "Request contains invalid or conflicting data.",
                400,
                LocalDateTime.now()
        );
        when(apiErrorResponseCreator.buildResponse(
                "Request contains invalid or conflicting data.",
                HttpStatus.BAD_REQUEST
        )).thenReturn(expectedResponse);

        ApiErrorResponse result = userExceptionHandler.handleDataIntegrityViolationException(exception);

        assertThat(result).isEqualTo(expectedResponse);
        verify(apiErrorResponseCreator).buildResponse(
                "Request contains invalid or conflicting data.",
                HttpStatus.BAD_REQUEST
        );
    }

    @Test
    @DisplayName("Should join validation messages with distinct field names")
    void shouldHandleValidationExceptions() throws NoSuchMethodException {
        var target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "firstName", "must not be blank"));
        bindingResult.addError(new FieldError("target", "lastName", "must not be blank"));
        bindingResult.addError(new FieldError("target", "firstName", "size must be between 2 and 50"));
        Method method = UserExceptionHandlerTest.class.getDeclaredMethod("shouldHandleValidationExceptions");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, -1), bindingResult);
        ApiErrorResponse expectedResponse = new ApiErrorResponse(
                "must not be blank and must not be blank and size must be between 2 and 50",
                400,
                LocalDateTime.now()
        );
        when(apiErrorResponseCreator.buildResponse(
                "must not be blank and must not be blank and size must be between 2 and 50",
                HttpStatus.BAD_REQUEST
        )).thenReturn(expectedResponse);

        ApiErrorResponse result = userExceptionHandler.handleValidationExceptions(exception);

        assertThat(result).isEqualTo(expectedResponse);
        verify(apiErrorResponseCreator).buildResponse(
                "must not be blank and must not be blank and size must be between 2 and 50",
                HttpStatus.BAD_REQUEST
        );
    }
}
