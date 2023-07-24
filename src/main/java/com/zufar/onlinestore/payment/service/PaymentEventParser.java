package com.zufar.onlinestore.payment.service;

import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventParser {

    public PaymentIntent parseEventToPaymentIntent(Event event) {
        log.info("parse event to payment intent: preparation for payment event deserialization: event: {}", event);
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElseThrow();
        log.info("parse event to payment intent: receiving stripe object: stripeObject: {}", stripeObject);
        if (stripeObject instanceof PaymentIntent paymentIntent) {
            return paymentIntent;
        } else {
            return null;
        }
    }
}
