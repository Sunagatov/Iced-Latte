package com.zufar.icedlatte.review.ai;

public class ReviewModerationException extends RuntimeException {

    public ReviewModerationException(String reason) {
        super("Review rejected by moderation: " + reason);
    }
}
