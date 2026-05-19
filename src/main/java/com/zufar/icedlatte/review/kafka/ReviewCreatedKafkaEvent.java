package com.zufar.icedlatte.review.kafka;

import com.zufar.icedlatte.review.api.ReviewCreatedEvent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReviewCreatedKafkaEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        String sourceApp,
        Instant occurredAt,
        String correlationId,
        UUID actorId,
        Payload payload
) {

    public ReviewCreatedKafkaEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(sourceApp, "sourceApp");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(payload, "payload");
    }

    public static ReviewCreatedKafkaEvent fromDomainEvent(ReviewCreatedEvent event) {
        Payload payload = new Payload(event.reviewId(), event.productId(), event.text());
        return new ReviewCreatedKafkaEvent(
                event.eventId(), "review.created", 1, "iced-latte", event.occurredAt(), null, null, payload
        );
    }

    public ReviewCreatedEvent toDomainEvent() {
        return new ReviewCreatedEvent(eventId, payload.reviewId(), payload.text(), payload.productId(), occurredAt);
    }

    public record Payload(UUID reviewId, UUID productId, String text) {
        public Payload {
            Objects.requireNonNull(reviewId, "reviewId");
            Objects.requireNonNull(productId, "productId");
        }
    }
}
