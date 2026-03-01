package com.zufar.icedlatte.review.ai;

public interface ReviewSummarizationService {

    /**
     * Generates a concise AI summary for a review.
     *
     * @return summary sentence, or a fallback string if AI is unavailable
     */
    String summarize(String text, int rating);
}
