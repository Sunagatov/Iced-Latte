package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReviewProcessingService {

    private final ReviewModerationService moderationService;
    private final ReviewSummarizationService summarizationService;
    private final ProductReviewRepository reviewRepository;

    @Async
    @Transactional
    public void process(UUID reviewId, String text, int rating) {
        try {
            moderationService.moderate(text);
        } catch (ReviewModerationException e) {
            log.warn("Review {} failed moderation, deleting. reason={}", reviewId, e.getMessage());
            reviewRepository.deleteById(reviewId);
            return;
        }

        reviewRepository.findById(reviewId).ifPresent(review -> {
            var summary = summarizationService.summarize(text, rating);
            review.setAiSummary(summary);
            reviewRepository.save(review);
            log.debug("Review {} processed: summary stored.", reviewId);
        });
    }
}
