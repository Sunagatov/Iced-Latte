package com.zufar.icedlatte.review.messaging.kafka.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewCreatedOutboxEventListener")
class ReviewCreatedOutboxEventListenerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("writes review-created envelope using the original domain event id")
    void writesReviewCreatedEnvelopeUsingOriginalDomainEventId() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        KafkaIntegrationProperties properties = new KafkaIntegrationProperties(
                true,
                new KafkaIntegrationProperties.Topics("iced-latte.review.created.v1"),
                new KafkaIntegrationProperties.ConsumerGroups("iced-latte-review-ai"),
                new KafkaIntegrationProperties.Outbox(true, true, 25, 10,
                        java.time.Duration.ofSeconds(5), java.time.Duration.ofMinutes(5),
                        java.time.Duration.ofSeconds(10), "test-outbox-worker"),
                KafkaIntegrationProperties.Inbox.defaults()
        );
        var listener = new ReviewCreatedOutboxEventListener(objectMapper, properties, outboxEventRepository);
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent domainEvent = new ReviewCreatedEvent(
                eventId, reviewId, "private review text", productId, Instant.parse("2026-05-24T12:00:00Z"));

        listener.writeOutboxEvent(domainEvent);

        ArgumentCaptor<ReviewCreatedKafkaEvent> eventCaptor = ArgumentCaptor.forClass(ReviewCreatedKafkaEvent.class);
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxEventRepository).insertReviewCreatedEvent(
                eventCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("iced-latte.review.created.v1"),
                org.mockito.ArgumentMatchers.eq(productId.toString()),
                payloadCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("{}"),
                org.mockito.ArgumentMatchers.eq(10)
        );
        assertThat(eventCaptor.getValue().eventId()).isEqualTo(eventId);
        assertThat(eventCaptor.getValue().payload().reviewId()).isEqualTo(reviewId);
        assertThat(eventCaptor.getValue().payload().productId()).isEqualTo(productId);
        assertThat(payloadCaptor.getValue())
                .contains("\"eventId\":\"" + eventId + "\"")
                .doesNotContain("private review text", "\"text\"");
    }
}
