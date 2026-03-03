package com.zufar.icedlatte.review.api;

import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.ProductReviewNotFoundException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReviewProvider unit tests")
class ProductReviewProviderTest {

    @Mock
    private ProductReviewRepository reviewRepository;
    @InjectMocks
    private ProductReviewProvider provider;

    @Test
    @DisplayName("Returns review entity when found")
    void getReviewEntityById_found_returnsEntity() {
        UUID reviewId = UUID.randomUUID();
        ProductReview review = ProductReview.builder().id(reviewId).build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        ProductReview result = provider.getReviewEntityById(reviewId);

        assertThat(result).isEqualTo(review);
    }

    @Test
    @DisplayName("Throws ProductReviewNotFoundException when review not found")
    void getReviewEntityById_notFound_throwsProductReviewNotFoundException() {
        UUID reviewId = UUID.randomUUID();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.getReviewEntityById(reviewId))
                .isInstanceOf(ProductReviewNotFoundException.class)
                .hasMessageContaining(reviewId.toString());
    }
}
