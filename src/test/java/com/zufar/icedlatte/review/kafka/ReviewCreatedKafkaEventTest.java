package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.api.ReviewCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReviewCreatedKafkaEvent")
class ReviewCreatedKafkaEventTest {

    @Test
    @DisplayName("maps domain review-created event into versioned Kafka envelope")
    void mapsDomainEventIntoVersionedKafkaEnvelope() {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-05-18T12:00:00Z");
        ReviewCreatedEvent domainEvent = new ReviewCreatedEvent(eventId, reviewId, "Fresh review", productId, occurredAt);

        ReviewCreatedKafkaEvent kafkaEvent = ReviewCreatedKafkaEvent.fromDomainEvent(domainEvent);

        assertThat(kafkaEvent.eventId()).isEqualTo(eventId);
        assertThat(kafkaEvent.eventType()).isEqualTo("review.created");
        assertThat(kafkaEvent.eventVersion()).isEqualTo(1);
        assertThat(kafkaEvent.sourceApp()).isEqualTo("iced-latte");
        assertThat(kafkaEvent.occurredAt()).isEqualTo(occurredAt);
        assertThat(kafkaEvent.payload().reviewId()).isEqualTo(reviewId);
        assertThat(kafkaEvent.payload().productId()).isEqualTo(productId);
        assertThat(kafkaEvent.payload().text()).isEqualTo("Fresh review");
    }

    @Test
    @DisplayName("maps Kafka envelope back to domain event for existing processing service")
    void mapsKafkaEnvelopeBackToDomainEvent() {
        UUID eventId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-05-18T12:00:00Z");
        ReviewCreatedKafkaEvent kafkaEvent = new ReviewCreatedKafkaEvent(
                eventId,
                "review.created",
                1,
                "iced-latte",
                occurredAt,
                null,
                null,
                new ReviewCreatedKafkaEvent.Payload(reviewId, productId, "Fresh review")
        );

        ReviewCreatedEvent domainEvent = kafkaEvent.toDomainEvent();

        assertThat(domainEvent.eventId()).isEqualTo(eventId);
        assertThat(domainEvent.reviewId()).isEqualTo(reviewId);
        assertThat(domainEvent.productId()).isEqualTo(productId);
        assertThat(domainEvent.text()).isEqualTo("Fresh review");
        assertThat(domainEvent.occurredAt()).isEqualTo(occurredAt);
    }
}
