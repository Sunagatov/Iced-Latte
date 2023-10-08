package com.zufar.onlinestore.payment.api.impl.scenario;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

import static com.zufar.onlinestore.openapi.payment.dto.ProcessedPaymentDetailsDto.StatusEnum.IS_SUCCEEDED;

/**
 * This class is responsible for handling the successful scenario and updating
 * in database record of payment, with the relevant status and description
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentSuccessfulScenarioExecutor implements PaymentScenarioExecutor {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void execute(PaymentIntent paymentIntent) {
        log.info("Handle payment scenario method: start of handling payment intent: {} by successful scenario.", paymentIntent);
        paymentRepository.updateStatusAndDescriptionInPayment(paymentIntent.getId(), IS_SUCCEEDED.toString(), IS_SUCCEEDED.getValue());
        log.info("Handle payment scenario method: finish of handling payment intent: {} by successful scenario.", paymentIntent);
    }

    @Override
    public boolean supports(Event event) {return Objects.equals(IS_SUCCEEDED.getValue(), event.getType());}
}
