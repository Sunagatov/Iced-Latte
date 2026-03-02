package com.zufar.icedlatte.review.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
@RequiredArgsConstructor
class LangChain4jReviewAiService implements ReviewModerationService, ReviewSummarizationService {

    private static final String OK = "OK";
    private static final String FALLBACK_SUMMARY = "Summary unavailable.";

    private final ReviewAiService reviewAiService;

    @Override
    public void moderate(String text) {
        try {
            var response = reviewAiService.moderate(text);
            if (!response.startsWith(OK)) {
                var reason = response.contains(":") ? response.substring(response.indexOf(':') + 1).trim() : response;
                throw new ReviewModerationException(reason);
            }
        } catch (ReviewModerationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI moderation unavailable, allowing review through. cause={}", e.getMessage());
        }
    }

    @Override
    public String summarize(String text, int rating) {
        try {
            return reviewAiService.summarize(text, rating);
        } catch (Exception e) {
            log.warn("AI summarization unavailable, using fallback. cause={}", e.getMessage());
            return FALLBACK_SUMMARY;
        }
    }
}
