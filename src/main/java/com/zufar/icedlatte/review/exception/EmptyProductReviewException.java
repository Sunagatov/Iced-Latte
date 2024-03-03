package com.zufar.icedlatte.review.exception;

public class EmptyProductReviewException extends RuntimeException {
    public EmptyProductReviewException() {
        super("Invalid review text, make sure it's not empty");
    }
}
