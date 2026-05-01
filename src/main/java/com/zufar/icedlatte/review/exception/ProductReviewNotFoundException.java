package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public class ProductReviewNotFoundException extends RuntimeException {

    public ProductReviewNotFoundException(UUID productReviewId) {
        super(String.format("Product's review with productReviewId = '%s' was not found", productReviewId));
    }

    public ProductReviewNotFoundException(UUID productId, UUID userId) {
        super(String.format("Product's review for productId = '%s' and userId = '%s' was not found", productId, userId));
    }
}
