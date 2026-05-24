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

    private static final Set<String> SAFE_HEADER_NAMES =
            Set.of("eventId", "eventType", "eventVersion", "sourceApp", "correlationId", "contentType");

    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final InboxEventRepository inboxEventRepository;

    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment)
            throws JsonProcessingException {
        if (!properties.inbox().enabled()) {
            throw new IllegalStateException("Kafka inbox recording is disabled while Kafka consumer is active");
        }

        ReviewCreatedKafkaEvent event = objectMapper.readValue(record.value(), ReviewCreatedKafkaEvent.class);
        boolean inserted = inboxEventRepository.insertReceivedEvent(
                event,
                record.topic(),
                record.key(),
                record.partition(),
                record.offset(),
                properties.consumerGroups().reviewAi(),
                record.value(),
                safeHeadersAsJson(record),
                properties.inbox().maxAttempts());
        acknowledgment.acknowledge();

        if (inserted) {
            log.info(
                    "event.inbox.recorded: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(),
                    record.topic(),
                    record.partition(),
                    record.offset());
        } else {
            log.info(
                    "event.inbox.duplicate: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(),
                    record.topic(),
                    record.partition(),
                    record.offset());
        }
    }

    private String safeHeadersAsJson(ConsumerRecord<String, String> record) throws JsonProcessingException {
        Map<String, String> safeHeaders = new LinkedHashMap<>();
        record.headers().forEach(header -> {
            String key = header.key();
            String value = new String(header.value(), StandardCharsets.UTF_8);

            if (SAFE_HEADER_NAMES.contains(key)) {
                safeHeaders.put(key, value);
            }
        });
        return objectMapper.writeValueAsString(safeHeaders);
    }
}
