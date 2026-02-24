package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.review.exception.DeniedProductReviewDeletionException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.validator.ProductReviewValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    private ProductInfoRepository productInfoRepository;
    @InjectMocks
    private ProductReviewDeleter deleter;

    @Test
    @DisplayName("Deletes review and updates product stats")
    void delete_validRequest_deletesAndUpdatesStats() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        deleter.delete(productId, reviewId);

        verify(productReviewValidator).validateProductReviewDeletionAllowed(reviewId);
        verify(productReviewValidator).validateProductIdIsValid(productId, reviewId);
        verify(reviewRepository).deleteById(reviewId);
        verify(productInfoRepository).updateAverageRating(productId);
        verify(productInfoRepository).updateReviewsCount(productId);
    }

    @Test
    @DisplayName("Propagates DeniedProductReviewDeletionException when deletion not allowed")
    void delete_notOwner_throwsDeniedDeletionException() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        doThrow(new DeniedProductReviewDeletionException(userId, reviewId))
                .when(productReviewValidator).validateProductReviewDeletionAllowed(reviewId);

        assertThatThrownBy(() -> deleter.delete(productId, reviewId))
                .isInstanceOf(DeniedProductReviewDeletionException.class);
    }
}
