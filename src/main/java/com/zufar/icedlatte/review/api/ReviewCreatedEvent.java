package com.zufar.icedlatte.review.api;

import java.util.UUID;

public record ReviewCreatedEvent(UUID reviewId, String text, UUID productId) {}
