package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
@RequiredArgsConstructor
class LangChain4jReviewAiService implements ReviewModerationService, ProductSummaryService {

    private static final String OK = "OK";
    private static final String FALLBACK_SUMMARY = "Summary unavailable.";

    private final ReviewAiService reviewAiService;
    private final ProductReviewRepository reviewRepository;

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
    public String summarize(UUID productId) {
        try {
            var reviews = reviewRepository.findAllByProductId(productId);
            if (reviews.isEmpty()) return null;
            var combined = reviews.stream()
                    .map(r -> "- " + r.getText())
                    .collect(Collectors.joining("\n"));
            return reviewAiService.aggregateSummary(combined);
        } catch (Exception e) {
            log.warn("AI product summary unavailable. cause={}", e.getMessage());
            return FALLBACK_SUMMARY;
        }
    }
}
