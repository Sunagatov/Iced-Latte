package com.zufar.onlinestore.review.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ReviewNotFoundException extends RuntimeException {
    private final String reviewId;

    @Override
    public String getMessage() {
        return "Review with id " + reviewId + " not found.";
    }
}