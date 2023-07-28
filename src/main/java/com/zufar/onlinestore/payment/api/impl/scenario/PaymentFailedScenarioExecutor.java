package com.zufar.onlinestore.payment.api.impl.scenario;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

/**
 * This class is responsible for handling the fail scenario and updating
 * in database record of payment, with the relevant status and description
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentFailedScenarioExecutor implements PaymentScenarioExecutor {

    private final PaymentRepository paymentRepository;

    private static final PaymentStatus paymentStatus = PaymentStatus.PAYMENT_IS_FAILED;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void execute(PaymentIntent paymentIntent) {
        log.info("Handle payment scenario method: start of handling payment intent: {} by failed scenario.", paymentIntent);
        paymentRepository.updateStatusAndDescriptionInPayment(paymentIntent.getId(), paymentStatus.toString(), paymentStatus.getDescription());
        log.info("Handle payment scenario method: finish of handling payment intent: {} by failed scenario.", paymentIntent);
    }

    @Override
    public boolean supports(Event event) {
        return Objects.equals(paymentStatus.getStatus(), event.getType());
    }
}
