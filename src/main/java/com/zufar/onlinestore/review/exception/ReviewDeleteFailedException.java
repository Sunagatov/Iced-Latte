package com.zufar.onlinestore.review.exception;

import lombok.Getter;

@Getter
public class ReviewDeleteFailedException extends RuntimeException {
    private final String reviewId;

    public ReviewDeleteFailedException(String reviewId) {
        super(String.format("Failed to delete review with id %s.", reviewId));
        this.reviewId = reviewId;
    }
}