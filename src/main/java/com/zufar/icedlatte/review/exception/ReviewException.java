package com.zufar.icedlatte.review.exception;

/** Sealed base for all review-related exceptions. */
public abstract sealed class ReviewException extends RuntimeException
        permits ReviewModerationException, ReviewSummaryException {

    protected ReviewException(String message) {
        super(message);
    }

    protected ReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
