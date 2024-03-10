package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public class DeniedProductReviewCreationException extends RuntimeException {
    public DeniedProductReviewCreationException(final UUID userId, final UUID productReviewId, final UUID reviewId) {
        super(String.format("Creation of product review is denied for the user with ID '%s' and product with ID '%s'. Delete the previous review '%s' first.", userId, productReviewId, reviewId));
    }
}
