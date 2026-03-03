package com.zufar.icedlatte.review.ai;

public interface ReviewModerationService {

    /**
     * Validates review text against community guidelines.
     *
     * @throws ReviewModerationException if the content violates guidelines
     */
    void moderate(String text);
}
