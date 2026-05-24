package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.dto.ReviewCreatedEvent;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.lang.reflect.RecordComponent;
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
    }

    @Test
    @DisplayName("does not put review text in Kafka payload")
    void doesNotPutReviewTextInKafkaPayload() {
        UUID reviewId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ReviewCreatedEvent domainEvent = new ReviewCreatedEvent(reviewId, "Fresh review", productId);

        ReviewCreatedKafkaEvent kafkaEvent = ReviewCreatedKafkaEvent.fromDomainEvent(domainEvent);

        assertThat(kafkaEvent.payload().reviewId()).isEqualTo(reviewId);
        assertThat(kafkaEvent.payload().productId()).isEqualTo(productId);
        assertThat(ReviewCreatedKafkaEvent.Payload.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .doesNotContain("text");
    }
}
