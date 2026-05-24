package com.zufar.icedlatte.review.messaging.kafka.outbox;

import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class ReviewCreatedKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaIntegrationProperties properties;
    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelayString = "${kafka.outbox.poll-interval:PT5S}")
    public void publishPendingOutboxEvents() {
        if (!properties.outbox().enabled() || !properties.outbox().workerEnabled()) {
            return;
        }

        int reclaimed = outboxEventRepository.reclaimStaleLocks(
                Instant.now().minus(properties.outbox().staleLockTimeout())
        );
        if (reclaimed > 0) {
            log.warn("review.outbox.locks.reclaimed: count={}", reclaimed);
        }

        var events = outboxEventRepository.claimPublishableEvents(
                properties.outbox().batchSize(),
                properties.outbox().workerId()
        );
        for (var event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEventRepository.OutboxEventRow event) {
        try {
            SendResult<String, String> result = kafkaTemplate.send(
                    event.topic(),
                    event.partitionKey(),
                    event.payload()
            ).get(properties.outbox().publishTimeout().toMillis(), TimeUnit.MILLISECONDS);

            outboxEventRepository.markPublished(
                    event.id(),
                    properties.outbox().workerId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
            log.info("review.outbox.published: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(), event.topic(), result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            outboxEventRepository.markFailed(
                    event.id(),
                    properties.outbox().workerId(),
                    event.attemptCount(),
                    event.maxAttempts(),
                    e
            );
            log.warn("review.outbox.publish.failed: eventId={}, topic={}",
                    event.eventId(), event.topic(), e);
        }
    }
}
