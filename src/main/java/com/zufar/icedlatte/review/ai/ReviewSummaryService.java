package com.zufar.icedlatte.review.ai;

public interface ReviewSummaryService {
    /**
     * Generates a 1-2 sentence third-person summary of the given review text.
     * Returns null if generation fails or AI is disabled.
     */
    String summarize(String reviewText);
}
