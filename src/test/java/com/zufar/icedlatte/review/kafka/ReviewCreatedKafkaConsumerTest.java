package com.zufar.icedlatte.review.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.review.messaging.kafka.inbox.InboxEventRepository;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.inbox.ReviewCreatedKafkaConsumer;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCreatedKafkaConsumer")
class ReviewCreatedKafkaConsumerTest {

    @Mock
    private InboxEventRepository inboxEventRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @Test
    @DisplayName("records consumed event in inbox and acknowledges after insert")
    void recordsConsumedEventInInboxAndAcknowledgesAfterInsert() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedKafkaEvent event = new ReviewCreatedKafkaEvent(
                eventId,
                "review.created",
                1,
                "iced-latte",
                Instant.parse("2026-05-18T12:00:00Z"),
                null,
                null,
                new ReviewCreatedKafkaEvent.Payload(reviewId, productId)
        );
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String payload = objectMapper.writeValueAsString(event);
        var properties = new KafkaIntegrationProperties(
                true,
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                KafkaIntegrationProperties.Outbox.defaults(),
                new KafkaIntegrationProperties.Inbox(true, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), "test-inbox-worker")
        );
        var consumer = new ReviewCreatedKafkaConsumer(objectMapper, properties, inboxEventRepository);
        var record = new ConsumerRecord<>("iced-latte.review.created.v1", 0, 42L, productId.toString(), payload);
        when(inboxEventRepository.insertReceivedEvent(eq(event), eq("iced-latte.review.created.v1"),
                eq(productId.toString()), eq(0), eq(42L), eq("iced-latte-review-ai"), eq(payload), eq("{}"), eq(10)))
                .thenReturn(true);

        consumer.consume(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("does not acknowledge when inbox insert fails")
    void doesNotAcknowledgeWhenInboxInsertFails() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedKafkaEvent event = new ReviewCreatedKafkaEvent(
                eventId,
                "review.created",
                1,
                "iced-latte",
                Instant.parse("2026-05-18T12:00:00Z"),
                null,
                null,
                new ReviewCreatedKafkaEvent.Payload(reviewId, productId)
        );
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String payload = objectMapper.writeValueAsString(event);
        var properties = new KafkaIntegrationProperties(
                true,
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                KafkaIntegrationProperties.Outbox.defaults(),
                new KafkaIntegrationProperties.Inbox(true, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), "test-inbox-worker")
        );
        var consumer = new ReviewCreatedKafkaConsumer(objectMapper, properties, inboxEventRepository);
        var record = new ConsumerRecord<>("iced-latte.review.created.v1", 0, 42L, productId.toString(), payload);
        when(inboxEventRepository.insertReceivedEvent(eq(event), eq("iced-latte.review.created.v1"),
                eq(productId.toString()), eq(0), eq(42L), eq("iced-latte-review-ai"), eq(payload), eq("{}"), eq(10)))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> consumer.consume(record, acknowledgment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(acknowledgment, never()).acknowledge();
    }
}
