package com.zufar.onlinestore.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventProcessor {

    private final PaymentEventCreator paymentEventCreator;
    private final PaymentEventParser paymentEventParser;
    private final PaymentEventCatcher paymentEventCatcher;

    public void processPaymentEvent(String paymentIntentPayload, String stripeSignatureHeader) throws SignatureVerificationException {
        log.info("Process payment event: start payment event processing: input params paymentIntentPayload: {}," +
                "stripeSignatureHeader: {}.", paymentIntentPayload, stripeSignatureHeader);
        Event event = paymentEventCreator.createPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
        PaymentIntent paymentIntent = paymentEventParser.parseEventToPaymentIntent(event);
        paymentEventCatcher.catchPaymentEventType(event, paymentIntent);
        log.info("Process payment event: event successfully processed");
    }

}
