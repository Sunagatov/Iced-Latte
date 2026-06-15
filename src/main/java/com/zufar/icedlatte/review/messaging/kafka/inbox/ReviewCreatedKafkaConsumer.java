package com.zufar.icedlatte.review.messaging.kafka.inbox;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class ReviewCreatedKafkaConsumer {

    private static final String REVIEW_CREATED_EVENT_TYPE = "review.created";
    private static final int REVIEW_CREATED_EVENT_VERSION = 1;
    private static final Set<String> SAFE_HEADER_NAMES =
            Set.of("eventId", "eventType", "eventVersion", "sourceApp", "correlationId", "contentType");

    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final InboxEventRepository inboxEventRepository;

    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        if (!properties.inbox().enabled()) {
            throw new IllegalStateException("Kafka inbox recording is disabled while Kafka consumer is active");
        }

        String topic = record.topic();
        int partition = record.partition();
        long offset = record.offset();
        ReviewCreatedKafkaEvent event;
        try {
            event = objectMapper.readValue(record.value(), ReviewCreatedKafkaEvent.class);
        } catch (JsonProcessingException e) {
            String logMessage = "event.inbox.malformed: topic={}, partition={}, offset={}, exceptionClass={}";
            log.warn(logMessage, topic, partition, offset, e.getClass().getSimpleName());
            acknowledgment.acknowledge();
            return;
        }
        if (!REVIEW_CREATED_EVENT_TYPE.equals(event.eventType())
                || event.eventVersion() != REVIEW_CREATED_EVENT_VERSION) {
            String logMessage =
                    "event.inbox.unsupported: eventId={}, eventType={}, eventVersion={}, topic={}, partition={}, offset={}";
            log.warn(logMessage, event.eventId(), event.eventType(), event.eventVersion(), topic, partition, offset);
            acknowledgment.acknowledge();
            return;
        }
        boolean inserted = inboxEventRepository.insertReceivedEvent(
                event,
                topic,
                record.key(),
                partition,
                offset,
                properties.consumerGroups().reviewAi(),
                record.value(),
                safeHeadersAsJson(record),
                properties.inbox().maxAttempts());
        acknowledgment.acknowledge();

        if (inserted) {
            String logMessage = "event.inbox.recorded: eventId={}, topic={}, partition={}, offset={}";
            log.info(logMessage, event.eventId(), topic, partition, offset);
        } else {
            String logMessage = "event.inbox.duplicate: eventId={}, topic={}, partition={}, offset={}";
            log.info(logMessage, event.eventId(), topic, partition, offset);
        }
    }

    private String safeHeadersAsJson(ConsumerRecord<String, String> record) {
        Map<String, String> safeHeaders = new LinkedHashMap<>();
        record.headers().forEach(header -> {
            String key = header.key();

            if (SAFE_HEADER_NAMES.contains(key)) {
                String value = header.value() == null ? null : new String(header.value(), StandardCharsets.UTF_8);
                safeHeaders.put(key, value);
            }
        });
        try {
            return objectMapper.writeValueAsString(safeHeaders);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize safe Kafka headers", e);
        }
    }
}
