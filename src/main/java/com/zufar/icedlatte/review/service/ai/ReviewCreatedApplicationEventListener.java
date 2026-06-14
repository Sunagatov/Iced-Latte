package com.zufar.icedlatte.review.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ReviewCreatedApplicationEventListener {

    private final AsyncReviewProcessingService processingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        processingService.processByReviewId(event.reviewId());
    }
}
