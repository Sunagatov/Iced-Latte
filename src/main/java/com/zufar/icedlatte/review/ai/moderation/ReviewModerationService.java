package com.zufar.icedlatte.review.ai.moderation;

import com.zufar.icedlatte.review.exception.ReviewModerationException;

public interface ReviewModerationService {

    /**
     * Validates review text against community guidelines.
     *
     * @throws ReviewModerationException if the content violates guidelines
     */
    void moderate(String text);
}
