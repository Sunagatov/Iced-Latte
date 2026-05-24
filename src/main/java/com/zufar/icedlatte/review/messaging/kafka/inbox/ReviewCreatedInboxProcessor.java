package com.zufar.icedlatte.review.messaging.kafka.inbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import com.zufar.icedlatte.review.service.ai.AsyncReviewProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
class ReviewCreatedInboxProcessor {

    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final InboxEventRepository inboxEventRepository;
    private final AsyncReviewProcessingService processingService;

    @Scheduled(fixedDelayString = "${kafka.inbox.poll-interval:PT5S}")
    public void processPendingInboxEvents() {
        if (!properties.inbox().enabled() || !properties.inbox().workerEnabled()) {
            return;
        }

        int reclaimed = inboxEventRepository.reclaimStaleLocks(
                Instant.now().minus(properties.inbox().staleLockTimeout())
        );
        if (reclaimed > 0) {
            log.warn("review.inbox.locks.reclaimed: count={}", reclaimed);
        }

        var events = inboxEventRepository.claimProcessableEvents(
                properties.inbox().batchSize(),
                properties.inbox().workerId()
        );
        for (var event : events) {
            process(event);
        }
    }

    private void process(InboxEventRepository.InboxEventRow row) {
        try {
            ReviewCreatedKafkaEvent event = objectMapper.readValue(row.payload(), ReviewCreatedKafkaEvent.class);
            AsyncReviewProcessingService.ProcessingResult result =
                    processingService.processByReviewId(event.payload().reviewId());
            if (result == AsyncReviewProcessingService.ProcessingResult.IGNORED) {
                inboxEventRepository.markIgnored(row.id(), properties.inbox().workerId());
            } else {
                inboxEventRepository.markProcessed(row.id(), properties.inbox().workerId());
            }
            log.info("review.inbox.processed: eventId={}, status={}", row.eventId(), result);
        } catch (Exception e) {
            inboxEventRepository.markFailed(
                    row.id(),
                    properties.inbox().workerId(),
                    row.attemptCount(),
                    row.maxAttempts(),
                    e
            );
            log.warn("review.inbox.processing.failed: eventId={}", row.eventId(), e);
        }
    }
}
