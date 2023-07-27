package com.zufar.onlinestore.payment.service.event;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for payment event (stripe object) creation.
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventCreator {

    private final StripeConfiguration stripeConfig;

    public Event createPaymentEvent(String paymentIntentPayload, String stripeSignatureHeader) throws SignatureVerificationException {
        log.info("Create payment event: start payment event creation:" +
                " paymentIntentPayload: {}, stripeSignatureHeader: {}.", paymentIntentPayload, stripeSignatureHeader);
        return Webhook.constructEvent(paymentIntentPayload, stripeSignatureHeader, stripeConfig.webHookSecretKey());
    }
}
