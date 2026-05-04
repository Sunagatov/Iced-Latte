package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.payment.entity.StripeWebhookEvent;
import com.zufar.icedlatte.payment.entity.WebhookEventStatus;
import com.zufar.icedlatte.payment.repository.StripeWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Manages webhook event deduplication using insert-first pattern.
 * Uses REQUIRES_NEW so event status persists independently of business processing.
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // Spring injects this service and transaction boundaries are framework-managed.
public class StripeWebhookEventRecorder {

    private final StripeWebhookEventRepository repository;

    /**
     * Attempts to acquire the event for processing.
     * Insert and re-acquire run in separate REQUIRES_NEW transactions because
     * a failed saveAndFlush marks the Hibernate session rollback-only.
     * <p>
     * This method is NOT @Transactional — it coordinates two independent TXs.
     * Callers must invoke it from a non-transactional context (e.g., StripeWebhookService).
     */
    public boolean tryAcquire(String eventId, String eventType) {
        try {
            return tryInsertNewEvent(eventId, eventType);
        } catch (DataIntegrityViolationException e) {
            return tryReacquireRetryableEvent(eventId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryInsertNewEvent(String eventId, String eventType) {
        repository.saveAndFlush(new StripeWebhookEvent(
                eventId, eventType, WebhookEventStatus.PROCESSING,
                OffsetDateTime.now(), null, null));
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryReacquireRetryableEvent(String eventId) {
        return repository.findById(eventId)
                .filter(evt -> evt.getStatus() == WebhookEventStatus.RETRYABLE_FAILED)
                .map(evt -> {
                    evt.setStatus(WebhookEventStatus.PROCESSING);
                    evt.setFailureReason(null);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(String eventId) {
        repository.findById(eventId).ifPresent(e -> {
            e.setStatus(WebhookEventStatus.PROCESSED);
            e.setProcessedAt(OffsetDateTime.now());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRetryableFailed(String eventId, String reason) {
        repository.findById(eventId).ifPresent(e -> {
            e.setStatus(WebhookEventStatus.RETRYABLE_FAILED);
            e.setFailureReason(reason != null ? reason.substring(0, Math.min(reason.length(), 2000)) : null);
        });
    }
}
