package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import com.zufar.icedlatte.review.api.ReviewCreatedEvent;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncReviewProcessingService unit tests")
class AsyncReviewProcessingServiceTest {

    @Mock private ReviewModerationService moderationService;
    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductReviewProductGateway productReviewProductGateway;
    @Mock private ProductReviewSummaryDebouncer summaryDebouncer;

    @InjectMocks private AsyncReviewProcessingService service;

    @Test
    @DisplayName("does nothing else when moderation passes")
    void doesNothingElseWhenModerationPasses() {
        ReviewCreatedEvent event = new ReviewCreatedEvent(UUID.randomUUID(), "Great coffee", UUID.randomUUID());

        service.process(event);

        verify(moderationService).moderate("Great coffee");
        verifyNoInteractions(reviewRepository, productReviewProductGateway, summaryDebouncer);
    }

    @Test
    @DisplayName("deletes rejected reviews and refreshes product aggregates when the review still exists")
    void deletesRejectedReviewsAndRefreshesProductAggregatesWhenReviewStillExists() {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, "spam", productId);
        ProductReview review = ProductReview.builder().id(reviewId).productId(productId).build();
        doThrow(new ReviewModerationException("spam"))
                .when(moderationService).moderate("spam");
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        service.process(event);

        verify(reviewRepository).deleteById(reviewId);
        verify(productReviewProductGateway).refreshReviewAggregates(productId);
        verify(summaryDebouncer).schedule(productId);
    }

    @Test
    @DisplayName("stops after moderation failure when the review has already disappeared")
    void stopsAfterModerationFailureWhenReviewHasAlreadyDisappeared() {
        UUID reviewId = UUID.randomUUID();
        ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, "spam", UUID.randomUUID());
        doThrow(new ReviewModerationException("spam"))
                .when(moderationService).moderate("spam");
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        service.process(event);

        verify(reviewRepository).findById(reviewId);
        verifyNoInteractions(productReviewProductGateway, summaryDebouncer);
    }
}
