package com.zufar.icedlatte.review.service.ai;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.ai.moderation.ReviewModerationService;
import com.zufar.icedlatte.review.service.ai.summary.ProductReviewSummaryDebouncer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReviewProcessingService {

    private final ReviewModerationService moderationService;
    private final ProductReviewRepository reviewRepository;
    private final ProductReviewProductApi productReviewProductApi;
    private final ProductReviewSummaryDebouncer summaryDebouncer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(ReviewCreatedEvent event) {
        processByReviewId(event.reviewId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessingResult processByReviewId(UUID reviewId) {
        var review = reviewRepository.findById(reviewId);
        if (review.isEmpty()) {
            log.info("review.processing.ignored: reviewId={}, reason=REVIEW_NOT_FOUND", reviewId);
            return ProcessingResult.IGNORED;
        }

        UUID productId = review.get().getProductId();
        try {
            moderationService.moderate(review.get().getText());
        } catch (ReviewModerationException e) {
            log.warn("review.moderation.failed: reviewId={}, reasonCode=REJECTED_BY_MODERATION", reviewId);
            reviewRepository.deleteById(reviewId);
            productReviewProductApi.refreshReviewAggregates(productId);
            summaryDebouncer.schedule(productId);
            log.info("review.moderation.rejected: reviewId={}, productId={}", reviewId, productId);
        }
        return ProcessingResult.PROCESSED;
    }

    public enum ProcessingResult {
        PROCESSED,
        IGNORED
    }
}
