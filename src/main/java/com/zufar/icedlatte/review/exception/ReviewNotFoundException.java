package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public final class ReviewNotFoundException extends ReviewException {

    public ReviewNotFoundException(UUID productReviewId) {
        super(message(productReviewId));
    }

    public ReviewNotFoundException(UUID productId, UUID userId) {
        super(message(productId, userId));
    }

    private static String message(UUID productReviewId) {
        return String.format("Product's review with productReviewId = '%s' was not found", productReviewId);
    }

    private static String message(UUID productId, UUID userId) {
        return String.format(
                "Product's review for productId = '%s' and userId = '%s' was not found", productId, userId);
    }
}
