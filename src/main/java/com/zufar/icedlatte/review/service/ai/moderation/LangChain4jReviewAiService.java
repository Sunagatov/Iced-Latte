package com.zufar.icedlatte.review.service.ai.moderation;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.zufar.icedlatte.review.exception.ReviewModerationException;
import com.zufar.icedlatte.review.exception.ReviewSummaryException;
import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import com.zufar.icedlatte.review.service.ai.summary.ProductSummaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
@RequiredArgsConstructor
class LangChain4jReviewAiService implements ReviewModerationService, ProductSummaryService {

    private static final String OK = "OK";
    private static final int MAX_REVIEWS_FOR_SUMMARY = 100;

    private final ReviewAiService reviewAiService;
    private final ProductReviewRepository reviewRepository;

    @Override
    public void moderate(String text) {
        try {
            var response = reviewAiService.moderate(text);
            var normalizedResponse = response == null ? "" : response.trim();

            if (OK.equals(normalizedResponse)) {
                return;
            }
            String reason;
            if (normalizedResponse.contains(":")) {
                reason = normalizedResponse
                        .substring(normalizedResponse.indexOf(':') + 1)
                        .trim();
            } else {
                reason = normalizedResponse;
            }
            throw new ReviewModerationException(reason);
        } catch (ReviewModerationException e) {
            throw e;
        } catch (Exception e) {
            String logMessage = "ai.moderation.unavailable: exceptionClass={}";
            log.warn(logMessage, e.getClass().getSimpleName());
        }
    }

    @Override
    public String summarize(UUID productId) {
        try {
            PageRequest pageRequest =
                    PageRequest.of(0, MAX_REVIEWS_FOR_SUMMARY, Sort.by(Sort.Direction.DESC, "createdAt"));
            var reviews = reviewRepository.findAllByProductId(productId, pageRequest);
            if (reviews.isEmpty()) {
                return null;
            }
            var combined = reviews.stream().map(r -> "- " + r.getText()).collect(Collectors.joining("\n"));
            return reviewAiService.aggregateSummary(combined);
        } catch (Exception e) {
            String logMessage = "ai.summary.unavailable: productId={}, exceptionClass={}";
            log.warn(logMessage, productId, e.getClass().getSimpleName());
            throw new ReviewSummaryException(productId, e);
        }
    }
}
