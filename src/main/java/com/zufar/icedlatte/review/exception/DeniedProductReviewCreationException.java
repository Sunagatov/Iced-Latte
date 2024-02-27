package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public class DeniedProductReviewCreationException extends RuntimeException {
    public DeniedProductReviewCreationException(final UUID userId, final UUID productReviewId) {
        super(String.format("Creation of ProductReview is denied for the user with ID '%s' and product with ID '%s. Delete the previous review first.", userId, productReviewId));
    }
}
