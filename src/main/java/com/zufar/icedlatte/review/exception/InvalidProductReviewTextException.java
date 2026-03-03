package com.zufar.icedlatte.review.exception;

public class InvalidProductReviewTextException extends RuntimeException {
    public InvalidProductReviewTextException() {
        super("The Product Review Text Is Invalid.");
    }
}
