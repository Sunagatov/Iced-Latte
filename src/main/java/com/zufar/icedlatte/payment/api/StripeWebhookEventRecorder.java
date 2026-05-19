package com.zufar.icedlatte.payment.api;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Non-transactional coordinator for webhook event deduplication.
 * Delegates to {@link StripeWebhookEventTransactionService} so that each
 * REQUIRES_NEW transaction goes through the Spring proxy.
 */
@Service
@RequiredArgsConstructor
public class StripeWebhookEventRecorder {

    private final StripeWebhookEventTransactionService txService;

    public boolean tryAcquire(String eventId, String eventType) {
        try {
            return txService.tryInsertNewEvent(eventId, eventType);
        } catch (DataIntegrityViolationException _) {
            return txService.tryReacquireRetryableEvent(eventId);
        }
    }

    public void markProcessed(String eventId) {
        txService.markProcessed(eventId);
    }

    public void markRetryableFailed(String eventId, String reason) {
        txService.markRetryableFailed(eventId, reason);
    }
}
