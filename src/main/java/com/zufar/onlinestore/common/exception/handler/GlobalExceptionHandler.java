package com.zufar.onlinestore.common.exception.handler;

import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.user.exception.UserAlreadyRegisteredException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    protected static final String DESCRIPTION_TEMPLATE = "Operation was failed in method: %s that belongs to the class: %s." +
            " Problematic code line: %d";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValidException(final MethodArgumentNotValidException exception) {
        ApiResponse<Void> apiResponse = buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.error("Handle method argument not valid exception: failed: messages: {}, description: {}.",
                apiResponse.messages(), apiResponse.description());

        return apiResponse;
    }

    protected ApiResponse<Void> buildResponse(Exception exception, HttpStatus httpStatus) {
        return ApiResponse.<Void>builder()
                .messages(collectErrorMessages(exception))
                .description(buildErrorDescription(exception))
                .timestamp(LocalDateTime.now())
                .httpStatusCode(httpStatus.value())
                .build();
    }

    private List<String> collectErrorMessages(Exception exception) {
        if (exception instanceof UserAlreadyRegisteredException userAlreadyRegisteredException) {
            return userAlreadyRegisteredException.getErrors();
        }

        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            return methodArgumentNotValidException
                    .getBindingResult()
                    .getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
        }

        return List.of(exception.getMessage());
    }

    private String buildErrorDescription(Exception exception) {
        return Stream.of(exception.getStackTrace())
                .findFirst()
                .map(element -> DESCRIPTION_TEMPLATE.formatted(element.getMethodName(), element.getClassName(), element.getLineNumber()))
                .orElse(Strings.EMPTY);
    }
}

