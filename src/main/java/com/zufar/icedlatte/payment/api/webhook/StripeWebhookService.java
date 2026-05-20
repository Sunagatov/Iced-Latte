package com.zufar.icedlatte.payment.api.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Non-transactional webhook coordinator.
 * Parse → acquire event → delegate to business processor → mark result.
 * <p>
 * Iced Latte uses Stripe test mode only — no real money is charged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@SuppressWarnings("unused") // Spring injects this service and configuration fields are framework-managed.
public class StripeWebhookService {

    private final StripeWebhookEventRecorder webhookEventRecorder;
    private final StripeWebhookBusinessProcessor webhookBusinessProcessor;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public void processWebhook(String payload, String stripeSignature) {
        Event event = parseEvent(payload, stripeSignature);

        if (!webhookEventRecorder.tryAcquire(event.getId(), event.getType())) {
            log.info("payment.webhook.duplicate: eventId={}", event.getId());
            return;
        }

        try {
            webhookBusinessProcessor.process(event);
            // Business processor returns normally for both success and non-retryable
            // failures (e.g., RECONCILIATION_FAILED is persisted inside the TX).
            webhookEventRecorder.markProcessed(event.getId());
        } catch (Exception e) {
            // Only transient/unexpected failures reach this catch block.
            try {
                webhookEventRecorder.markRetryableFailed(event.getId(), e.getMessage());
            } catch (Exception markerFailure) {
                log.error("payment.webhook.failed_to_mark_failed: eventId={}",
                        event.getId(), markerFailure);
            }
            throw e; // Rethrow so Stripe retries
        }

        log.info("payment.webhook.processed: eventType={}, eventId={}",
                event.getType(), event.getId());
    }

    private Event parseEvent(String payload, String signature) {
        try {
            return Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException _) {
            log.warn("payment.webhook.signature_invalid");
            throw new PaymentEventProcessingException();
        }
    }
}
