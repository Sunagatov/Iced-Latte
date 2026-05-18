package com.zufar.icedlatte.review.api;

import java.time.Instant;
import java.util.UUID;

public record ReviewCreatedEvent(UUID eventId, UUID reviewId, String text, UUID productId, Instant occurredAt) {

    public ReviewCreatedEvent(UUID reviewId, String text, UUID productId) {
        this(UUID.randomUUID(), reviewId, text, productId, Instant.now());
    }
}
