package com.zufar.icedlatte.payment.service.webhook;

import com.stripe.model.Event;

interface StripeWebhookEventHandler {

    boolean supports(Event event);

    void handle(Event event);
}
