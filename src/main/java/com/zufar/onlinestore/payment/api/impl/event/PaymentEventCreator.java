package com.zufar.onlinestore.payment.api.impl.event;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.exception.PaymentEventProcessingException;
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

    public Event createPaymentEvent(String paymentIntentPayload, String stripeSignatureHeader) {
        log.info("Create payment event: start payment event creation:" +
                " paymentIntentPayload: {}, stripeSignatureHeader: {}.", paymentIntentPayload, stripeSignatureHeader);
        try {
            return Webhook.constructEvent(paymentIntentPayload, stripeSignatureHeader, stripeConfig.webHookSecretKey());
        } catch (SignatureVerificationException ex) {
            log.error("Error during payment event creating", ex);
            throw new PaymentEventProcessingException(stripeSignatureHeader);
        }
    }
}
