package com.zufar.icedlatte.payment.api.webhook;

import com.zufar.icedlatte.payment.entity.StripeWebhookEvent;
import com.zufar.icedlatte.payment.entity.WebhookEventStatus;
import com.zufar.icedlatte.payment.repository.StripeWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Transaction-boundary bean for webhook event persistence.
 * Each method runs in REQUIRES_NEW so the event state is committed
 * independently of the outer webhook-processing transaction.
 * <p>
 * This bean exists only to make Spring @Transactional proxy work —
 * self-invocation inside StripeWebhookEventRecorder would bypass it.
 */
@Service
@RequiredArgsConstructor
class StripeWebhookEventTransactionService {

    private final StripeWebhookEventRepository repository;

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
