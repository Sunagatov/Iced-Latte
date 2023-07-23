package com.zufar.onlinestore.payment.processor;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.model.Payment;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentEventProcessor {

    private final StripeConfiguration stripeConfig;
    private final PaymentRepository paymentRepository;

    public Event processEvent(String payload, String header) throws SignatureVerificationException {
        return Webhook.constructEvent(payload, header, stripeConfig.webHookSecretKey());
    }

    public void processSucceededPaymentIntent(String paymentIntentId) {
        updateDataTemplateMethod(paymentIntentId, PaymentStatus.SUCCEEDED);
    }

    public void processHandleFailedPaymentIntent(String paymentIntentId) {
        updateDataTemplateMethod(paymentIntentId, PaymentStatus.PAYMENT_FAILED);
    }

    private void updateDataTemplateMethod(String paymentIntentId, PaymentStatus status) {
        log.info("update data template method: payment intent id and status was transfer: status: {}, paymentIntentId: {}", status, paymentIntentId);
        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId);
        log.info("update data template method: payment retrieved by paymentIntentId: payment: {}", payment);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setDescription(PaymentStatus.SUCCEEDED.getMessage());
        Payment savedPayment = paymentRepository.save(payment);
        log.info("update data template method: updated payment data: savedPayment: {}", savedPayment);
    }

}
