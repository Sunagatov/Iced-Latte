package com.zufar.icedlatte.payment.service.webhook;

import java.util.UUID;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
class StripeWebhookEventObjects {

    static Session requireSession(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElseThrow(() -> {
                    String logMessage = "payment.webhook.session_missing: eventType={}, eventId={}";
                    log.warn(logMessage, event.getType(), event.getId());
                    return new IllegalStateException("Stripe webhook event session data is missing.");
                });
    }

    static UUID extractOrderId(Session session) {
        String orderId = session.getClientReferenceId();
        if (orderId == null) {
            orderId = session.getMetadata().get("orderId");
        }
        if (orderId == null) {
            throw new IllegalStateException("No orderId in Stripe session metadata");
        }
        return UUID.fromString(orderId);
    }
}
