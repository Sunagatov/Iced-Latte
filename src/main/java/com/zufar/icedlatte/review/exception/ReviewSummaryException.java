package com.zufar.icedlatte.review.exception;

import java.util.UUID;

public final class ReviewSummaryException extends ReviewException {

    public ReviewSummaryException(UUID productId, Throwable cause) {
        super("AI summary generation failed for product " + productId, cause);
    }
}
