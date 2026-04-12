package com.zufar.icedlatte.user.exception.handler;

import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.user.exception.DeliveryAddressNotFoundException;
import com.zufar.icedlatte.user.exception.InvalidAvatarFileTypeException;
import com.zufar.icedlatte.user.exception.InvalidOldPasswordException;
import com.zufar.icedlatte.user.exception.PutUsersBadRequestException;
import com.zufar.icedlatte.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class UserExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(DeliveryAddressNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleDeliveryAddressNotFoundException(final DeliveryAddressNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.warn("exception.delivery_address.not_found: exceptionClass={}, status=404", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(InvalidAvatarFileTypeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidAvatarFileTypeException(final InvalidAvatarFileTypeException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Invalid file type. Allowed types: JPEG, PNG, WebP", HttpStatus.BAD_REQUEST);
        log.warn("exception.avatar.invalid_type: status=400");
        return apiErrorResponse;
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleUserNotFoundException(final UserNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);
        log.warn("exception.user.not_found: exceptionClass={}, status=404", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleUsernameNotFoundException(final UsernameNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
        log.warn("exception.user.username_not_found: exceptionClass={}, status=401", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler({InvalidOldPasswordException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiErrorResponse handleInvalidOldPasswordException(final InvalidOldPasswordException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.UNAUTHORIZED);
        log.warn("exception.user.invalid_password: exceptionClass={}, status=401", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler({PutUsersBadRequestException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handlePutUsersBadRequestException(final PutUsersBadRequestException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.warn("exception.user.invalid_property: exceptionClass={}, status=400", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        String fieldNames = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getField)
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
        StringBuilder errorMessage = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            if (!errorMessage.isEmpty()) {
                errorMessage.append(" and ");
            }
            errorMessage.append(error.getDefaultMessage());
        });
        log.warn("exception.user.validation: fields={}, errorCount={}, status=400",
                fieldNames, ex.getBindingResult().getErrorCount());
        return apiErrorResponseCreator.buildResponse(errorMessage.toString(), HttpStatus.BAD_REQUEST);
    }
}
