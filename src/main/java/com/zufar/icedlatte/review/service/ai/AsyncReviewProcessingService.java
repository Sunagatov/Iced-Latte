package com.zufar.icedlatte.review.service.ai;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.product.api.ProductReviewProductApi;
import com.zufar.icedlatte.review.entity.ProductReview;
import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.ai.moderation.ReviewModerationService;
import com.zufar.icedlatte.review.service.ai.summary.ProductReviewSummaryDebouncer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReviewProcessingService {

    private final ReviewModerationService moderationService;
    private final ProductReviewRepository reviewRepository;
    private final ProductReviewProductApi productReviewProductApi;
    private final ProductReviewSummaryDebouncer summaryDebouncer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessingResult processByReviewId(UUID reviewId) {
        ProductReview review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            log.info("review.processing.ignored: reviewId={}, reason=REVIEW_NOT_FOUND", reviewId);
            return ProcessingResult.IGNORED;
        }

        UUID productId = review.getProductId();
        try {
            moderationService.moderate(review.getText());
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
