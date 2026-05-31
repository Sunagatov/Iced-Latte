package com.zufar.icedlatte.review.messaging.kafka.outbox;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class ReviewCreatedKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final OutboxEventRepository outboxEventRepository;

    private static final TypeReference<Map<String, String>> STRING_HEADERS = new TypeReference<>() {};

    @Scheduled(fixedDelayString = "${kafka.outbox.poll-interval:PT5S}")
    public void publishPendingOutboxEvents() {
        if (!properties.outbox().enabled() || !properties.outbox().workerEnabled()) {
            return;
        }

        int reclaimed = outboxEventRepository.reclaimStaleLocks(
                Instant.now().minus(properties.outbox().staleLockTimeout()));
        if (reclaimed > 0) {
            log.warn("review.outbox.locks.reclaimed: count={}", reclaimed);
        }

        var events = outboxEventRepository.claimPublishableEvents(
                properties.outbox().batchSize(), properties.outbox().workerId());
        for (var event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEventRepository.OutboxEventRow event) {
        KafkaIntegrationProperties.Outbox outbox = properties.outbox();
        try {
            log.info("event.outbox.publish.started: eventId={}, topic={}", event.eventId(), event.topic());
            SendResult<String, String> result = kafkaTemplate
                    .send(toProducerRecord(event))
                    .get(outbox.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);

            outboxEventRepository.markPublished(
                    event.id(),
                    outbox.workerId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            log.info(
                    "event.outbox.publish.succeeded: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(),
                    event.topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            outboxEventRepository.markFailed(
                    event.id(), outbox.workerId(), event.attemptCount(), event.maxAttempts(), e);
            log.warn("event.outbox.publish.failed: eventId={}, topic={}", event.eventId(), event.topic(), e);
        }
    }

    private ProducerRecord<String, String> toProducerRecord(OutboxEventRepository.OutboxEventRow event)
            throws JsonProcessingException {
        var record = new ProducerRecord<>(event.topic(), event.partitionKey(), event.payload());
        if (event.headers() == null || event.headers().isBlank()) {
            return record;
        }
        Map<String, String> headers = objectMapper.readValue(event.headers(), STRING_HEADERS);
        headers.forEach((key, value) ->
                record.headers().add(key, value == null ? null : value.getBytes(StandardCharsets.UTF_8)));
        return record;
    }
}
