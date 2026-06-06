package com.zufar.icedlatte.review.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ReviewCreatedEvent(UUID eventId, UUID reviewId, UUID productId, Instant occurredAt) {

    public ReviewCreatedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(reviewId, "reviewId");
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public ReviewCreatedEvent(UUID reviewId, UUID productId) {
        this(UUID.randomUUID(), reviewId, productId, Instant.now());
    }
}
