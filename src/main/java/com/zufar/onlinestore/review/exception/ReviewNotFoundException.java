package com.zufar.onlinestore.review.exception;

import lombok.Getter;

@Getter
public class ReviewNotFoundException extends RuntimeException {
    private final String reviewId;

    public ReviewNotFoundException(String reviewId) {
        super(String.format("Review with id %s not found.", reviewId));
        this.reviewId = reviewId;
    }
}