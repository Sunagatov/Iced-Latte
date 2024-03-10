package com.zufar.icedlatte.review.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.handler.ErrorDebugMessageCreator;
import com.zufar.icedlatte.review.exception.RatingConverterException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class RatingExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;
    private final ErrorDebugMessageCreator errorDebugMessageCreator;

    @ExceptionHandler({RatingConverterException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleRatingConverterException(final RatingConverterException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR);
        log.warn("Handle rating converter exception: failed: message: {}, debugMessage: {}",
                apiErrorResponse.message(), errorDebugMessageCreator.buildErrorDebugMessage(exception));
        return apiErrorResponse;
    }
}
