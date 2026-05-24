package com.zufar.icedlatte.review.messaging.kafka.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
class ReviewCreatedOutboxEventListener {

    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final OutboxEventRepository outboxEventRepository;

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

        outboxEventRepository.insertReviewCreatedEvent(kafkaEvent, topic, partitionKey, payload, "{}", maxAttempts);
    }
}
