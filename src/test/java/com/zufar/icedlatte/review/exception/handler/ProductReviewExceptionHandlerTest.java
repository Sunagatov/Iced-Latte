package com.zufar.icedlatte.review.exception.handler;

import com.zufar.icedlatte.common.exception.dto.ApiErrorResponse;
import com.zufar.icedlatte.common.exception.handler.ApiErrorResponseCreator;
import com.zufar.icedlatte.review.exception.DeniedProductReviewCreationException;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.exception.EmptyProductReviewException;
import com.zufar.icedlatte.review.exception.GetReviewsBadRequestException;
import com.zufar.icedlatte.review.exception.InvalidProductReviewTextException;
import com.zufar.icedlatte.review.exception.ProductNotFoundForReviewException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewExceptionHandler unit tests")
class ProductReviewExceptionHandlerTest {

    @Mock
    private ApiErrorResponseCreator apiErrorResponseCreator;
    @InjectMocks
    private ProductReviewExceptionHandler handler;

    private ApiErrorResponse stubResponse(String message) {
        return new ApiErrorResponse(message, 400, LocalDateTime.now());
    }

    @Test
    @DisplayName("handleUnsupportedReviewFormatException returns BAD_REQUEST response")
    void handleEmptyReview_returnsBadRequest() {
        EmptyProductReviewException ex = new EmptyProductReviewException();
        ApiErrorResponse expected = stubResponse("Product's review is empty");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);

        ApiErrorResponse result = handler.handleUnsupportedReviewFormatException(ex);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleDeniedProductReviewDeletionException returns BAD_REQUEST response")
    void handleDeniedDeletion_returnsBadRequest() {
        UUID userId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        DeniedProductReviewDeletionException ex = new DeniedProductReviewDeletionException(userId, reviewId);
        ApiErrorResponse expected = stubResponse("Deletion denied");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);

        ApiErrorResponse result = handler.handleDeniedProductReviewDeletionException(ex);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleDeniedProductReviewCreationException returns BAD_REQUEST response")
    void handleDeniedCreation_returnsBadRequest() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        DeniedProductReviewCreationException ex = new DeniedProductReviewCreationException(userId, productId, reviewId);
        ApiErrorResponse expected = stubResponse("Creation denied");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);

        ApiErrorResponse result = handler.handleDeniedProductReviewCreationException(ex);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleProductNotFoundForReviewException returns NOT_FOUND response")
    void handleProductNotFound_returnsNotFound() {
        UUID productId = UUID.randomUUID();
        ProductNotFoundForReviewException ex = new ProductNotFoundForReviewException(productId);
        ApiErrorResponse expected = new ApiErrorResponse("Product not found", 404, LocalDateTime.now());
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.NOT_FOUND)).thenReturn(expected);

        ApiErrorResponse result = handler.handleProductNotFoundForReviewException(ex);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleGetReviewsBadRequestException returns BAD_REQUEST response")
    void handleGetReviewsBadRequest_returnsBadRequest() {
        GetReviewsBadRequestException ex = new GetReviewsBadRequestException("bad params");
        ApiErrorResponse expected = stubResponse("bad params");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);

        ApiErrorResponse result = handler.handleGetReviewsBadRequestException(ex);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("handleInvalidProductReviewTextException returns BAD_REQUEST response")
    void handleInvalidTextReturnsBadRequest() {
        var ex = new InvalidProductReviewTextException();
        ApiErrorResponse expected = stubResponse("The Product Review Text Is Invalid.");
        when(apiErrorResponseCreator.buildResponse(ex, HttpStatus.BAD_REQUEST)).thenReturn(expected);

        ApiErrorResponse result = handler.handleInvalidProductReviewTextException(ex);

        assertThat(result).isEqualTo(expected);
    }
}
