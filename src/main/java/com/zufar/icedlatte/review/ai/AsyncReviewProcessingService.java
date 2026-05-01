package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.product.api.ProductReviewProductGateway;
import com.zufar.icedlatte.review.api.ReviewCreatedEvent;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReviewProcessingService {

    private final ReviewModerationService moderationService;
    private final ProductReviewRepository reviewRepository;
    private final ProductReviewProductGateway productReviewProductGateway;
    private final ProductReviewSummaryDebouncer summaryDebouncer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
                productReviewProductGateway.refreshReviewAggregates(productId);
                summaryDebouncer.schedule(productId);
                log.info("review.moderation.rejected: reviewId={}, productId={}", reviewId, productId);
            });
        }
    }
}
