package com.zufar.onlinestore.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventRetriever {

    private final StripeConfiguration stripeConfig;

    public Event retrievePaymentEvent(String paymentIntentPayload, String stripeSignatureHeader) throws SignatureVerificationException {
        log.info("retrieve payment event: preparation for payment event construct:" +
                " paymentIntentPayload: {}, stripeSignatureHeader: {}.", paymentIntentPayload, stripeSignatureHeader);
        return Webhook.constructEvent(paymentIntentPayload, stripeSignatureHeader, stripeConfig.webHookSecretKey());
    }
}
