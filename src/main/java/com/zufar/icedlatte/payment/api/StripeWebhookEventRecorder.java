package com.zufar.icedlatte.payment.api;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Non-transactional coordinator for webhook event deduplication.
 * Delegates to {@link StripeWebhookEventTxHelper} so that each
 * REQUIRES_NEW transaction goes through the Spring proxy.
 */
@Service
@RequiredArgsConstructor
public class StripeWebhookEventRecorder {

    private final StripeWebhookEventTxHelper txHelper;

    public boolean tryAcquire(String eventId, String eventType) {
        try {
            return txHelper.tryInsertNewEvent(eventId, eventType);
        } catch (DataIntegrityViolationException e) {
            return txHelper.tryReacquireRetryableEvent(eventId);
        }
    }

    public void markProcessed(String eventId) {
        txHelper.markProcessed(eventId);
    }

    public void markRetryableFailed(String eventId, String reason) {
        txHelper.markRetryableFailed(eventId, reason);
    }
}
