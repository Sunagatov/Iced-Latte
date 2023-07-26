package com.zufar.onlinestore.payment.service;

import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Service
public class PaymentFailedScenarioHandler implements PaymentScenarioHandler {

    private PaymentRepository paymentRepository;

    public void handlePaymentScenario(final PaymentIntent paymentIntent) {
        log.info("Handle payment scenario method: start of handling payment intent: {} by failed scenario.", paymentIntent);
        PaymentStatus status = PaymentStatus.PAYMENT_IS_FAILED;
        Payment updatedPayment = paymentRepository.updateStatusAndDescriptionInPayment(
                paymentIntent.getId(), status, status.getDescription());
        log.info("Handle payment scenario method: finish of handling payment intent: {} by failed scenario.", paymentIntent);
        log.debug("Handle payment scenario method: payment data was updated: {}.", updatedPayment);
    }
}
