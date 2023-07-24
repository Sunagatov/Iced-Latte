package com.zufar.onlinestore.payment.service;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentEventHandler {

    private final PaymentRepository paymentRepository;

    public void handleEvent(Event event, PaymentIntent paymentIntent) {
        if (!Objects.isNull(paymentIntent) && !Objects.isNull(event)) {
            String paymentIntentId = paymentIntent.getId();
            String eventType = event.getType();
            log.info("handle event: preparation for payment event status handling: " +
                    "paymentIntentId: {}, eventType: {}.", paymentIntentId, eventType);

            if (Objects.equals(PaymentStatus.PAYMENT_IS_SUCCEEDED.getStatus(), eventType)) {
                changePaymentDetailsAccordingStatus(paymentIntentId, PaymentStatus.PAYMENT_IS_SUCCEEDED);
            } else if (Objects.equals(PaymentStatus.PAYMENT_IS_FAILED.getStatus(), eventType)) {
                changePaymentDetailsAccordingStatus(paymentIntentId, PaymentStatus.PAYMENT_IS_FAILED);
            }
        }
    }

    private void changePaymentDetailsAccordingStatus(String paymentIntentId, PaymentStatus status) {
        log.info("update data template method: payment intent id and status was transfer: status: {}, paymentIntentId: {}", status, paymentIntentId);
        Payment payment = paymentRepository.findByPaymentIntentId(paymentIntentId);
        log.info("update data template method: payment retrieved by paymentIntentId: payment: {}", payment);
        payment.setStatus(status);
        payment.setDescription(status.getMessage());
        Payment savedPayment = paymentRepository.save(payment);
        log.info("update data template method: updated payment data: savedPayment: {}", savedPayment);
    }
}
