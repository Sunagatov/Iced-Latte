package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public class ProductNotFoundForReviewException extends RuntimeException {

    public ProductNotFoundForReviewException(UUID productId) {
        super(String.format("Product with productId = '%s' was not found. " +
                "Product's review operations (update, delete, provide) are not possible.", productId));
    }
}
