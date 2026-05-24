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
import java.util.UUID;

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
        KafkaIntegrationProperties.Inbox inbox = properties.inbox();
        if (!inbox.enabled() || !inbox.workerEnabled()) {
            return;
        }
        Instant lockedBefore = Instant.now().minus(inbox.staleLockTimeout());
        int reclaimed = inboxEventRepository.reclaimStaleLocks(lockedBefore);
        if (reclaimed > 0) {
            log.warn("review.inbox.locks.reclaimed: count={}", reclaimed);
        }
        String consumerName = properties.consumerGroups().reviewAi();

        var events = inboxEventRepository.claimProcessableEvents(inbox.batchSize(), consumerName, inbox.workerId());
        for (var event : events) {
            process(event);
        }
    }

    private void process(InboxEventRepository.InboxEventRow row) {
        KafkaIntegrationProperties.Inbox inbox = properties.inbox();
        try {
            log.info("event.inbox.processing.started: eventId={}", row.eventId());
            ReviewCreatedKafkaEvent event = objectMapper.readValue(row.payload(), ReviewCreatedKafkaEvent.class);
            UUID reviewId = event.payload().reviewId();
            AsyncReviewProcessingService.ProcessingResult result = processingService.processByReviewId(reviewId);
            if (result == AsyncReviewProcessingService.ProcessingResult.IGNORED) {
                inboxEventRepository.markIgnored(row.id(), inbox.workerId());
            } else {
                inboxEventRepository.markProcessed(row.id(), inbox.workerId());
            }
            log.info("event.inbox.processing.succeeded: eventId={}, status={}", row.eventId(), result);
        } catch (Exception e) {
            inboxEventRepository.markFailed(row.id(), inbox.workerId(), row.attemptCount(), row.maxAttempts(), e);
            log.warn("event.inbox.processing.failed: eventId={}", row.eventId(), e);
        }
    }
}
