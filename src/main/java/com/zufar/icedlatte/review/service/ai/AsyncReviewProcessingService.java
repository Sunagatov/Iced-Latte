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
        try {
            moderationService.moderate(event.text());
        } catch (ReviewModerationException e) {
            UUID reviewId = event.reviewId();
            log.warn("review.moderation.failed: reviewId={}, reasonCode=REJECTED_BY_MODERATION", reviewId);
            reviewRepository.findById(reviewId).ifPresent(review -> {
                UUID productId = review.getProductId();
                reviewRepository.deleteById(reviewId);
                productReviewProductApi.refreshReviewAggregates(productId);
                summaryDebouncer.schedule(productId);
                log.info("review.moderation.rejected: reviewId={}, productId={}", reviewId, productId);
            });
        }
    }
}
