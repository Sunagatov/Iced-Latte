package com.zufar.icedlatte.review.ai;

import com.zufar.icedlatte.review.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReviewProcessingService {

    private final ReviewModerationService moderationService;
    private final ProductReviewRepository reviewRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID reviewId, String text) {
        try {
            moderationService.moderate(text);
        } catch (ReviewModerationException e) {
            log.warn("review.moderation.failed: reviewId={} reason={}", reviewId, e.getMessage());
            reviewRepository.deleteById(reviewId);
        }
    }
}
