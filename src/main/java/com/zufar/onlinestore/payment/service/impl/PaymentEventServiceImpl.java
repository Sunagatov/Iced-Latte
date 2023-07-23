package com.zufar.onlinestore.payment.service.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.processor.PaymentEventProcessor;
import com.zufar.onlinestore.payment.service.PaymentEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventServiceImpl implements PaymentEventService {

    private final PaymentEventProcessor paymentEventProcessor;

    public void processPaymentEvents(String payload, String header) throws SignatureVerificationException {
        Event event = paymentEventProcessor.processEvent(payload, header);
        PaymentIntent paymentIntent = parseEventToPaymentIntent(event);
        String paymentIntentId = paymentIntent.getId();
        String paymentType = event.getType();

        if (paymentType.equals(PaymentStatus.SUCCEEDED.getStatus())) {
            paymentEventProcessor.processSucceededPaymentIntent(paymentIntentId);
        } else if (paymentType.equals(PaymentStatus.PAYMENT_FAILED.getStatus())) {
            paymentEventProcessor.processHandleFailedPaymentIntent(paymentIntentId);
        }
    }

    private PaymentIntent parseEventToPaymentIntent(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        PaymentIntent paymentIntent = null;
        if (dataObjectDeserializer.getObject().isPresent()) {
            paymentIntent = (PaymentIntent) dataObjectDeserializer.getObject().get();
        }
        return paymentIntent;
    }

}
