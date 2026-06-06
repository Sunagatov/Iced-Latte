package com.zufar.icedlatte.review.exception;

public final class ReviewModerationException extends ReviewException {

    public ReviewModerationException(String reason) {
        super("Review rejected by moderation: " + reason);
    }
}
