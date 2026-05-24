package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.outbox.OutboxEventRepository;
import com.zufar.icedlatte.review.messaging.kafka.outbox.ReviewCreatedKafkaPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCreatedKafkaPublisher")
class ReviewCreatedKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ReviewCreatedKafkaPublisher publisher;

    @BeforeEach
    void setUp() {
        KafkaIntegrationProperties properties = new KafkaIntegrationProperties(
                true,
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                new KafkaIntegrationProperties.Outbox(true, true, 25, 10, Duration.ofSeconds(5),
                        Duration.ofMinutes(5), Duration.ofSeconds(10), "test-outbox-worker"),
                KafkaIntegrationProperties.Inbox.defaults()
        );
        publisher = new ReviewCreatedKafkaPublisher(kafkaTemplate, properties, outboxEventRepository);
    }

    @Test
    @DisplayName("publishes claimed outbox rows and marks them published after Kafka ack")
    void publishesClaimedOutboxRowsAndMarksThemPublishedAfterKafkaAck() {
        UUID eventId = UUID.randomUUID();
        UUID rowId = UUID.randomUUID();
        var row = new OutboxEventRepository.OutboxEventRow(
                rowId,
                eventId,
                "iced-latte.review.created.v1",
                "product-1",
                "{\"eventId\":\"" + eventId + "\"}",
                "{}",
                0,
                10
        );
        when(outboxEventRepository.claimPublishableEvents(anyInt(), anyString())).thenReturn(List.of(row));
        var producerRecord = new ProducerRecord<>("iced-latte.review.created.v1", "product-1", row.payload());
        var metadata = new RecordMetadata(new TopicPartition("iced-latte.review.created.v1", 0),
                42L, 0, 0L, 0, 0);
        when(kafkaTemplate.send("iced-latte.review.created.v1", "product-1", row.payload()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(producerRecord, metadata)));

        publisher.publishPendingOutboxEvents();

        verify(kafkaTemplate).send("iced-latte.review.created.v1", "product-1", row.payload());
        verify(outboxEventRepository).markPublished(rowId, "test-outbox-worker", 0, 42L);
    }
}
