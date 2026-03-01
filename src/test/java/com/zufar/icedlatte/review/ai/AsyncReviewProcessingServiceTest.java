package com.zufar.icedlatte.review.ai;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncReviewProcessingService unit tests")
class AsyncReviewProcessingServiceTest {

    @Mock
    private ReviewModerationService moderationService;
    @Mock
    private ReviewSummarizationService summarizationService;
    @Mock
    private ProductReviewRepository reviewRepository;

    @InjectMocks
    private AsyncReviewProcessingService service;

    @Test
    @DisplayName("process: stores summary when moderation passes")
    void process_moderationPasses_storesSummary() {
        var reviewId = UUID.randomUUID();
        var review = ProductReview.builder().id(reviewId).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(summarizationService.summarize("Great!", 5)).thenReturn("Excellent review.");

        service.process(reviewId, "Great!", 5);

        assertThat(review.getAiSummary()).isEqualTo("Excellent review.");
        verify(reviewRepository).save(review);
        verify(reviewRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("process: deletes review when moderation fails")
    void process_moderationFails_deletesReview() {
        var reviewId = UUID.randomUUID();

        doThrow(new ReviewModerationException("spam")).when(moderationService).moderate("buy now!!!");

        service.process(reviewId, "buy now!!!", 1);

        verify(reviewRepository).deleteById(reviewId);
        verify(summarizationService, never()).summarize(any(), anyInt());
    }

    @Test
    @DisplayName("process: stores fallback summary when summarization fails")
    void process_summarizationFails_storesFallback() {
        var reviewId = UUID.randomUUID();
        var review = ProductReview.builder().id(reviewId).build();

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(summarizationService.summarize("Good", 4)).thenReturn("Summary unavailable.");

        service.process(reviewId, "Good", 4);

        assertThat(review.getAiSummary()).isEqualTo("Summary unavailable.");
        verify(reviewRepository).save(review);
    }
}
