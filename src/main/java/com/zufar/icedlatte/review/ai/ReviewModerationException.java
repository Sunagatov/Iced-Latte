package com.zufar.icedlatte.review.ai;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
public class ReviewModerationException extends RuntimeException {

    public ReviewModerationException(String reason) {
        super("Review rejected by moderation: " + reason);
    }
}
