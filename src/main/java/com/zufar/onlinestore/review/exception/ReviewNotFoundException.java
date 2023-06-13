package com.zufar.onlinestore.review.exception;

import lombok.Getter;

@Getter
public class ReviewNotFoundException extends RuntimeException {
    private final Long reviewId;

    public ReviewNotFoundException(Long reviewId) {
        super(String.format("Review with id %s not found.", reviewId));
        this.reviewId = reviewId;
    }
}