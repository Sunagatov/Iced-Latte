package com.zufar.icedlatte.payment.service.webhook;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional webhook business logic, extracted into a separate bean to ensure @Transactional is honored (avoids
 * Spring self-invocation trap).
 *
 * <p>Non-retryable business failures (e.g., amount mismatch) are persisted and the method returns normally — no throw
 * inside @Transactional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // Spring injects this bean; webhook flow enters through framework-managed calls.
public class StripeWebhookBusinessProcessor {

    private final List<StripeWebhookEventHandler> handlers;

    @Transactional
    public void process(Event event) {
        for (StripeWebhookEventHandler handler : handlers) {
            if (handler.supports(event)) {
                handler.handle(event);
                return;
            }
        }
        log.debug("payment.webhook.unhandled: eventType={}", event.getType());
    }
}
