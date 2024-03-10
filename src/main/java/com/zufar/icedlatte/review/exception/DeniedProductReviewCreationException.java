package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public class DeniedProductReviewCreationException extends RuntimeException {
    public DeniedProductReviewCreationException(final UUID userId, final UUID productId, final UUID productReviewId) {
        super(String.format("Creation of product's review is denied for the user with userId = '%s' and product with productId = '%s'. Delete the previous product's review '%s' first.", userId, productId, productReviewId));
    }
}
