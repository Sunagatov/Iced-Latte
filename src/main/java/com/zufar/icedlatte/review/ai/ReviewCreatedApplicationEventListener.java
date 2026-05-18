package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.review.api.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ReviewCreatedApplicationEventListener {

    private final AsyncReviewProcessingService processingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        processingService.process(event);
    }
}
