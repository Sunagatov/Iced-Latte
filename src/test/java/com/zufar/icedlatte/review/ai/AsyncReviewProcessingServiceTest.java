package com.zufar.icedlatte.review.ai;

import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.ai.AsyncReviewProcessingService;
import com.zufar.icedlatte.review.service.ai.moderation.ReviewModerationService;
import com.zufar.icedlatte.review.service.ai.summary.ProductReviewSummaryDebouncer;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncReviewProcessingService unit tests")
class AsyncReviewProcessingServiceTest {

    @Mock
    private ReviewModerationService moderationService;

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductReviewProductApi productReviewProductGateway;

    @Mock
    private ProductReviewSummaryDebouncer summaryDebouncer;

    @InjectMocks
    private AsyncReviewProcessingService service;

    @Test
    @DisplayName("does nothing else when moderation passes")
    void doesNothingElseWhenModerationPasses() {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, productId);
        ProductReview review = ProductReview.builder()
                .id(reviewId)
                .productId(productId)
                .text("Great coffee")
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        service.process(event);

        verify(reviewRepository).findById(reviewId);
        verify(moderationService).moderate("Great coffee");
        verifyNoInteractions(productReviewProductGateway, summaryDebouncer);
    }

    @Test
    @DisplayName("deletes rejected reviews and refreshes product aggregates when the review still exists")
    void deletesRejectedReviewsAndRefreshesProductAggregatesWhenReviewStillExists() {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, productId);
        ProductReview review = ProductReview.builder()
                .id(reviewId)
                .productId(productId)
                .text("spam")
                .build();
        doThrow(new ReviewModerationException("spam")).when(moderationService).moderate("spam");
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        service.process(event);

        verify(reviewRepository).deleteById(reviewId);
        verify(productReviewProductGateway).refreshReviewAggregates(productId);
        verify(summaryDebouncer).schedule(productId);
    }

    @Test
    @DisplayName("ignores processing when the review has already disappeared")
    void ignoresProcessingWhenReviewHasAlreadyDisappeared() {
        UUID reviewId = UUID.randomUUID();
        ReviewCreatedEvent event = new ReviewCreatedEvent(reviewId, UUID.randomUUID());
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        service.process(event);

        verify(reviewRepository).findById(reviewId);
        verifyNoInteractions(moderationService);
        verifyNoInteractions(productReviewProductGateway, summaryDebouncer);
    }
}
