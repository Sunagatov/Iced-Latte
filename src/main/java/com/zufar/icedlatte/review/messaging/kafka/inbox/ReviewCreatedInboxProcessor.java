package com.zufar.icedlatte.review.messaging.kafka.inbox;

import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zufar.icedlatte.common.monitoring.SentryHandledExceptionReporter;
import com.zufar.icedlatte.common.monitoring.SentryJobMonitor;
import com.zufar.icedlatte.review.messaging.kafka.config.KafkaIntegrationProperties;
import com.zufar.icedlatte.review.messaging.kafka.event.ReviewCreatedKafkaEvent;
import com.zufar.icedlatte.review.service.ai.AsyncReviewProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
public class ReviewCreatedInboxProcessor {

    private static final String REVIEW_CREATED_EVENT_TYPE = "review.created";
    private static final String MONITOR_SLUG = "review-created-inbox-processor";

    private final ObjectMapper objectMapper;
    private final KafkaIntegrationProperties properties;
    private final InboxEventRepository inboxEventRepository;
    private final AsyncReviewProcessingService processingService;
    private final SentryJobMonitor sentryJobMonitor;
    private final SentryHandledExceptionReporter sentryHandledExceptionReporter;

    @Scheduled(fixedDelayString = "${kafka.inbox.poll-interval:PT5S}")
    public void processPendingInboxEvents() {
        sentryJobMonitor.run(
                MONITOR_SLUG,
                sentryJobMonitor.fixedDelayConfig(
                        properties.inbox().pollInterval().toMillis()),
                this::processPendingInboxEventsInternal);
    }

    void processPendingInboxEventsInternal() {
        KafkaIntegrationProperties.Inbox inbox = properties.inbox();
        if (!inbox.enabled() || !inbox.workerEnabled()) {
            return;
        }
        Instant lockedBefore = Instant.now().minus(inbox.staleLockTimeout());
        String consumerName = properties.consumerGroups().reviewAi();
        int reclaimed = inboxEventRepository.reclaimStaleLocks(lockedBefore, consumerName, REVIEW_CREATED_EVENT_TYPE);
        if (reclaimed > 0) {
            log.warn("review.inbox.locks.reclaimed: count={}", reclaimed);
        }

        var events = inboxEventRepository.claimProcessableEvents(
                inbox.batchSize(), consumerName, REVIEW_CREATED_EVENT_TYPE, inbox.workerId());
        for (var event : events) {
            process(event);
        }
    }

    private void process(InboxEventRepository.InboxEventRow row) {
        KafkaIntegrationProperties.Inbox inbox = properties.inbox();
        String workerId = inbox.workerId();
        UUID rowId = row.id();
        String consumerName = properties.consumerGroups().reviewAi();
        String eventType = REVIEW_CREATED_EVENT_TYPE;
        UUID eventId = row.eventId();
        try {
            log.info("event.inbox.processing.started: eventId={}", eventId);
            ReviewCreatedKafkaEvent event = objectMapper.readValue(row.payload(), ReviewCreatedKafkaEvent.class);
            UUID reviewId = event.payload().reviewId();

            AsyncReviewProcessingService.ProcessingResult result = processingService.processByReviewId(reviewId);

            switch (result) {
                case IGNORED -> inboxEventRepository.markIgnored(rowId, workerId, consumerName, eventType);
                case PROCESSED -> inboxEventRepository.markProcessed(rowId, workerId, consumerName, eventType);
            }
            log.info("event.inbox.processing.succeeded: eventId={}, status={}", eventId, result);
        } catch (Exception e) {
            int attemptCount = row.attemptCount();
            int maxAttempts = row.maxAttempts();

            inboxEventRepository.markFailed(rowId, workerId, consumerName, eventType, attemptCount, maxAttempts, e);
            sentryHandledExceptionReporter.capture(e, scope -> {
                scope.setTag("component", "review-kafka-inbox");
                scope.setTag("operation", "process-review-created-event");
                scope.setExtra("eventId", eventId.toString());
                scope.setExtra("rowId", rowId.toString());
                scope.setExtra("workerId", workerId);
                scope.setExtra("consumerName", consumerName);
                scope.setExtra("attemptCount", Integer.toString(attemptCount));
                scope.setExtra("maxAttempts", Integer.toString(maxAttempts));
            });
            log.warn("event.inbox.processing.failed: eventId={}", eventId, e);
        }
    }
}
