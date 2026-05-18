package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.api.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class ReviewCreatedKafkaPublisher {

    private final KafkaTemplate<String, ReviewCreatedKafkaEvent> kafkaTemplate;

    @Value("${kafka.topics.review-created}")
    private String reviewCreatedTopic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(ReviewCreatedEvent event) {
        ReviewCreatedKafkaEvent kafkaEvent = ReviewCreatedKafkaEvent.fromDomainEvent(event);
        String key = event.productId().toString();

        kafkaTemplate.send(reviewCreatedTopic, key, kafkaEvent)
                .whenComplete((_, failure) -> {
                    if (failure != null) {
                        log.warn("review.kafka.publish.failed: topic={}, eventId={}, reviewId={}, productId={}",
                                reviewCreatedTopic, event.eventId(), event.reviewId(), event.productId(), failure);
                        return;
                    }
                    log.info("review.kafka.published: topic={}, eventId={}, reviewId={}, productId={}",
                            reviewCreatedTopic, event.eventId(), event.reviewId(), event.productId());
                });
    }
}
