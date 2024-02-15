package com.zufar.icedlatte.review.exception;

public class UnsupportedReviewFormatException extends RuntimeException {
    public UnsupportedReviewFormatException() {
        super("Invalid review text, make sure it's not empty");
    }
}
