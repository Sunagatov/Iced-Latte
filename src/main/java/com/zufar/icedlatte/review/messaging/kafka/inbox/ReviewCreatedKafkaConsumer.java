package com.zufar.icedlatte.review.messaging.kafka.inbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class ReviewCreatedKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final InboxEventRepository inboxEventRepository;

    @KafkaListener(
            topics = "${kafka.topics.review-created}",
            groupId = "${kafka.consumer-groups.review-ai}"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws JsonProcessingException {
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
                "{}",
                properties.inbox().maxAttempts()
        );
        acknowledgment.acknowledge();

        if (inserted) {
            log.info("review.inbox.received: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(), record.topic(), record.partition(), record.offset());
        } else {
            log.info("review.inbox.duplicate: eventId={}, topic={}, partition={}, offset={}",
                    event.eventId(), record.topic(), record.partition(), record.offset());
        }
    }
}
