package com.zufar.icedlatte.review.exception;

public final class ReviewAccessDeniedException extends ReviewException {

    public ReviewAccessDeniedException() {
        super("Access denied.");
    }
}
