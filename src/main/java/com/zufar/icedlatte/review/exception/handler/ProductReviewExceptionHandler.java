package com.zufar.icedlatte.review.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.common.exception.handler.ErrorDebugMessageCreator;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.exception.GetReviewsBadRequestException;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ProductReviewExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;
    private final ErrorDebugMessageCreator errorDebugMessageCreator;

    @ExceptionHandler(EmptyProductReviewException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnsupportedReviewFormatException(final EmptyProductReviewException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("Handle unsupported review format exception: failed: message: {}, debugMessage: {}.",
                apiErrorResponse.message(), errorDebugMessageCreator.buildErrorDebugMessage(exception));

        return apiErrorResponse;
    }

    @ExceptionHandler(DeniedProductReviewDeletionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleDeniedProductReviewDeletionException(final DeniedProductReviewDeletionException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("Handle denied product review deletion exception: failed: message: {}, debugMessage: {}.",
                apiErrorResponse.message(), errorDebugMessageCreator.buildErrorDebugMessage(exception));

        return apiErrorResponse;
    }

    @ExceptionHandler(DeniedProductReviewCreationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleDeniedProductReviewCreationException(final DeniedProductReviewCreationException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("Handle denied product review creation exception: failed: message: {}, debugMessage: {}.",
                apiErrorResponse.message(), errorDebugMessageCreator.buildErrorDebugMessage(exception));

        return apiErrorResponse;
    }

    @ExceptionHandler(ProductNotFoundForReviewException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleProductNotFoundForReviewException(final ProductNotFoundForReviewException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("Handle product not found for the provided review exception: failed: message: {}, debugMessage: {}.",
                apiErrorResponse.message(), errorDebugMessageCreator.buildErrorDebugMessage(exception));

        return apiErrorResponse;
    }

    @ExceptionHandler(GetReviewsBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGetReviewsBadRequestException(final GetReviewsBadRequestException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("Handle get product's reviews invalid parameters exception: failed: message: {}, debugMessage: {}.",
                apiErrorResponse.message(), errorDebugMessageCreator.buildErrorDebugMessage(exception));

        return apiErrorResponse;
    }
}
