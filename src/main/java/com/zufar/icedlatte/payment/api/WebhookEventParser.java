package com.zufar.icedlatte.payment.api;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for parsing payment event to session object.
 */
@Slf4j
@Service
public class WebhookEventParser {

    public Session parseEventToSession(final Event stripePaymentEvent) {
        log.debug("payment.webhook.parse: eventType={}", stripePaymentEvent.getType());

        return stripePaymentEvent.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElseGet(() -> {
                    log.debug("payment.webhook.parse.skipped: eventType={}", stripePaymentEvent.getType());
                    return null;
                });
    }
}
