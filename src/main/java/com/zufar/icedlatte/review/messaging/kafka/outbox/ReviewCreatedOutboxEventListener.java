package com.zufar.icedlatte.review.messaging.kafka.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.monitoring.SentryHandledExceptionReporter;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
class ReviewCreatedOutboxEventListener {

    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final OutboxEventRepository outboxEventRepository;
    private final SentryHandledExceptionReporter sentryHandledExceptionReporter;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void writeOutboxEvent(ReviewCreatedEvent event) throws JsonProcessingException {
        if (!properties.outbox().enabled()) {
            throw new IllegalStateException("Kafka outbox recording is disabled while Kafka integration is active");
        }

        ReviewCreatedKafkaEvent kafkaEvent = ReviewCreatedKafkaEvent.fromDomainEvent(event);
        String topic = properties.topics().reviewCreated();
        String payload = objectMapper.writeValueAsString(kafkaEvent);
        String partitionKey = event.productId().toString();
        int maxAttempts = properties.outbox().maxAttempts();

        try {
            outboxEventRepository.insertReviewCreatedEvent(kafkaEvent, topic, partitionKey, payload, "{}", maxAttempts);
        } catch (RuntimeException ex) {
            sentryHandledExceptionReporter.capture(ex, scope -> {
                scope.setTag("component", "review-kafka-outbox");
                scope.setTag("operation", "create-review-created-outbox-event");
                scope.setExtra("eventId", kafkaEvent.eventId().toString());
                scope.setExtra("reviewId", event.reviewId().toString());
                scope.setExtra("productId", event.productId().toString());
                scope.setExtra("topic", topic);
                scope.setExtra("partitionKey", partitionKey);
                scope.setExtra("maxAttempts", Integer.toString(maxAttempts));
            });
            throw ex;
        }
        String logMessage = "review.outbox.created: eventId={}, topic={}, partitionKey={}";
        log.info(logMessage, kafkaEvent.eventId(), topic, partitionKey);
    }
}
