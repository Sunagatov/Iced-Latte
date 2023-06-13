package com.zufar.onlinestore.review.exception;

import lombok.Getter;

@Getter
public class ReviewDeleteFailedException extends RuntimeException {
    private final Long reviewId;

    public ReviewDeleteFailedException(Long reviewId) {
        super(String.format("Failed to delete review with id %s.", reviewId));
        this.reviewId = reviewId;
    }
}