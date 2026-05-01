package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewDeleter unit tests")
class ProductReviewDeleterTest {

    @Mock
    private ProductReviewRepository reviewRepository;
    @Mock
    private ProductReviewValidator productReviewValidator;
    @Mock
    private ProductReviewProductGateway productReviewProductGateway;
    @Mock
    private com.zufar.icedlatte.review.ai.ProductReviewSummaryDebouncer summaryDebouncer;
    private ProductReviewDeleter deleter;

    @BeforeEach
    void setUp() {
        deleter = new ProductReviewDeleter(
                reviewRepository,
                productReviewValidator,
                productReviewProductGateway,
                summaryDebouncer
        );
    }

    @Test
    @DisplayName("Deletes review and updates product stats")
    void delete_validRequest_deletesAndUpdatesStats() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        deleter.delete(productId, reviewId, userId);

        verify(productReviewValidator).validateProductReviewDeletionAllowed(reviewId, userId);
        verify(productReviewValidator).validateProductIdIsValid(productId, reviewId);
        verify(reviewRepository).deleteById(reviewId);
        verify(productReviewProductGateway).refreshReviewAggregates(productId);
        verify(summaryDebouncer).schedule(productId);
    }

    @Test
    @DisplayName("Propagates BadRequestException when deletion not allowed")
    void delete_notOwner_throwsBadRequestException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new BadRequestException("Deletion denied"))
                .when(productReviewValidator).validateProductReviewDeletionAllowed(reviewId, userId);

        assertThatThrownBy(() -> deleter.delete(productId, reviewId, userId))
                .isInstanceOf(BadRequestException.class);
    }
}
