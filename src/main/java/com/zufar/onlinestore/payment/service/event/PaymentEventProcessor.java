package com.zufar.onlinestore.payment.service.event;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for processing payment event to transfer it to the responsibility area
 * of class that is engaged in catching event types.
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventProcessor {

    private final PaymentEventCreator paymentEventCreator;
    private final PaymentEventParser paymentEventParser;
    private final PaymentEventHandler paymentEventHandler;

    public void processPaymentEvent(String paymentIntentPayload, String stripeSignatureHeader) {
        log.info("Process payment event: start payment event processing: input params paymentIntentPayload: {}," +
                "stripeSignatureHeader: {}.", paymentIntentPayload, stripeSignatureHeader);
        Event event = paymentEventCreator.createPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
        PaymentIntent paymentIntent = paymentEventParser.parseEventToPaymentIntent(event);
        paymentEventHandler.handlePaymentEvent(event, paymentIntent);
        log.info("Process payment event: event successfully processed");
    }

}
