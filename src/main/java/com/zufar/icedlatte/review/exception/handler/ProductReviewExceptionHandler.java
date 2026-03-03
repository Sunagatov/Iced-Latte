package com.zufar.icedlatte.review.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.InvalidProductReviewTextException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.exception.GetReviewsBadRequestException;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(0)
public class ProductReviewExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(EmptyProductReviewException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnsupportedReviewFormatException(final EmptyProductReviewException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("exception.review.empty: message={}", apiErrorResponse.message());

        return apiErrorResponse;
    }

    @ExceptionHandler(InvalidProductReviewTextException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidProductReviewTextException(final InvalidProductReviewTextException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("exception.review.invalid_text: message={}", apiErrorResponse.message());

        return apiErrorResponse;
    }

    @ExceptionHandler(DeniedProductReviewDeletionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleDeniedProductReviewDeletionException(final DeniedProductReviewDeletionException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("exception.review.deletion_denied: message={}", apiErrorResponse.message());

        return apiErrorResponse;
    }

    @ExceptionHandler(DeniedProductReviewCreationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleDeniedProductReviewCreationException(final DeniedProductReviewCreationException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("exception.review.creation_denied: message={}", apiErrorResponse.message());

        return apiErrorResponse;
    }

    @ExceptionHandler(ProductNotFoundForReviewException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductNotFoundForReviewException(final ProductNotFoundForReviewException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);

        log.warn("exception.review.product_not_found: message={}", apiErrorResponse.message());

        return apiErrorResponse;
    }

    @ExceptionHandler(GetReviewsBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGetReviewsBadRequestException(final GetReviewsBadRequestException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.warn("exception.review.invalid_params: message={}", apiErrorResponse.message());

        return apiErrorResponse;
    }
}
