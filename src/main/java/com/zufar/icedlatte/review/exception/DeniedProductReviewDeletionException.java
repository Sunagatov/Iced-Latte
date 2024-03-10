package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public class DeniedProductReviewDeletionException extends RuntimeException {

    public DeniedProductReviewDeletionException(final UUID userId, final UUID productReviewId) {
        super(String.format("Deletion of ProductReview with ID '%s' is denied for the user with ID '%s'", productReviewId, userId));
    }
}
