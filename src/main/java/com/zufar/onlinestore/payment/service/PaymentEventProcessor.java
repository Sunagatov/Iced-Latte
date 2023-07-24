package com.zufar.onlinestore.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventProcessor {

    private final PaymentEventRetriever paymentEventRetriever;
    private final PaymentEventParser paymentEventParser;
    private final PaymentEventHandler paymentEventHandler;

    public void processPaymentEvent(String paymentIntentPayload, String stripeSignatureHeader) throws SignatureVerificationException {
        log.info("process payment event: preparation for payment event processing: paymentIntentPayload: {}," +
                "stripeSignatureHeader: {}.", paymentIntentPayload, stripeSignatureHeader);
        Event event = paymentEventRetriever.retrievePaymentEvent(paymentIntentPayload, stripeSignatureHeader);
        PaymentIntent paymentIntent = paymentEventParser.parseEventToPaymentIntent(event);
        paymentEventHandler.handleEvent(event, paymentIntent);
    }

}
