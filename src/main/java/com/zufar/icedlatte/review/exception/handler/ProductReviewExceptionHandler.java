package com.zufar.icedlatte.review.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.InvalidProductReviewTextException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.exception.GetReviewsBadRequestException;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.zufar.icedlatte.review")
@RequiredArgsConstructor
@Order(0)
public class ProductReviewExceptionHandler {

    private final ApiErrorResponseCreator apiErrorResponseCreator;

    @ExceptionHandler(EmptyProductReviewException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleUnsupportedReviewFormatException(final EmptyProductReviewException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.debug("exception.review.empty: exceptionClass={}, status=400", exception.getClass().getSimpleName());

        return apiErrorResponse;
    }

    @ExceptionHandler(InvalidProductReviewTextException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleInvalidProductReviewTextException(final InvalidProductReviewTextException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.debug("exception.review.invalid_text: exceptionClass={}, status=400", exception.getClass().getSimpleName());

        return apiErrorResponse;
    }

    @ExceptionHandler(DeniedProductReviewDeletionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleDeniedProductReviewDeletionException(final DeniedProductReviewDeletionException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.debug("exception.review.deletion_denied: exceptionClass={}, status=400", exception.getClass().getSimpleName());

        return apiErrorResponse;
    }

    @ExceptionHandler(DeniedProductReviewCreationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleDeniedProductReviewCreationException(final DeniedProductReviewCreationException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);

        log.debug("exception.review.creation_denied: exceptionClass={}, status=400", exception.getClass().getSimpleName());

        return apiErrorResponse;
    }

    @ExceptionHandler(ProductNotFoundForReviewException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductNotFoundForReviewException(final ProductNotFoundForReviewException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);

        log.debug("exception.review.product_not_found: exceptionClass={}, status=404", exception.getClass().getSimpleName());

        return apiErrorResponse;
    }

    @ExceptionHandler(ProductReviewNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleProductReviewNotFoundException(final ProductReviewNotFoundException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.NOT_FOUND);

        log.debug("exception.review.not_found: exceptionClass={}, status=404", exception.getClass().getSimpleName());

        return apiErrorResponse;
    }

    @ExceptionHandler(GetReviewsBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleGetReviewsBadRequestException(final GetReviewsBadRequestException exception) {
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(exception, HttpStatus.BAD_REQUEST);
        log.debug("exception.review.invalid_params: exceptionClass={}, status=400", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleDataIntegrityViolationException(final DataIntegrityViolationException exception) {
        // Concurrent duplicate review or duplicate like/dislike from the same user.
        // The DB unique constraints prevent data corruption; map to a clean 400 instead of 500.
        ApiErrorResponse apiErrorResponse = apiErrorResponseCreator.buildResponse(
                "Request conflicts with an existing record.", HttpStatus.BAD_REQUEST);
        log.debug("exception.review.duplicate: exceptionClass={}, status=400", exception.getClass().getSimpleName());
        return apiErrorResponse;
    }
}
