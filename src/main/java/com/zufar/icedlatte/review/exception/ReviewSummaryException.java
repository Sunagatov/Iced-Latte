package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public class ReviewSummaryException extends RuntimeException {

    public ReviewSummaryException(UUID productId, Throwable cause) {
        super("AI summary generation failed for product " + productId, cause);
    }
}
