package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.ai.AsyncReviewProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class ReviewCreatedKafkaConsumer {

    private final AsyncReviewProcessingService processingService;

    @KafkaListener(
            topics = "${kafka.topics.review-created}",
            groupId = "${kafka.consumer-groups.review-ai}"
    )
    public void consume(ReviewCreatedKafkaEvent event) {
        log.info(
                "review.kafka.consumed: eventId={}, reviewId={}, productId={}",
                event.eventId(),
                event.payload().reviewId(),
                event.payload().productId()
        );
        processingService.process(event.toDomainEvent());
    }
}
