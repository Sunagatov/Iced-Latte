package com.zufar.onlinestore.payment.service.scenario;

import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for handling the successful scenario and updating
 * in database record of payment, with the relevant status and description
 * */

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@Service
public class PaymentSuccessfulScenarioHandler implements PaymentScenarioHandler {

    private PaymentRepository paymentRepository;

    public void handlePaymentScenario(final PaymentIntent paymentIntent) {
        log.info("Handle payment scenario method: start of handling payment intent: {} by successful scenario.", paymentIntent);
        PaymentStatus status = PaymentStatus.PAYMENT_IS_SUCCEEDED;
        Payment updatedPayment = paymentRepository.updateStatusAndDescriptionInPayment(
                paymentIntent.getId(), status, status.getDescription());
        log.info("Handle payment scenario method: finish of handling payment intent: {} by successful scenario.", paymentIntent);
        log.debug("Handle payment scenario method: payment data was updated: {}.", updatedPayment);
    }
}
