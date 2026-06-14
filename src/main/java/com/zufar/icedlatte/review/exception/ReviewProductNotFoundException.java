package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public final class ReviewProductNotFoundException extends ReviewException {

    public ReviewProductNotFoundException(UUID productId) {
        super(message(productId));
    }

    private static String message(UUID productId) {
        return String.format(
                "Product with productId = '%s' was not found. Product's review operations are not possible.",
                productId);
    }
}
