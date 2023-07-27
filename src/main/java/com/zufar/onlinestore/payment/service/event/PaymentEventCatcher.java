package com.zufar.onlinestore.payment.service.event;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.exception.UnexpectedPaymentStatusException;
import com.zufar.onlinestore.payment.service.scenario.PaymentFailedScenarioHandler;
import com.zufar.onlinestore.payment.service.scenario.PaymentScenarioHandler;
import com.zufar.onlinestore.payment.service.scenario.PaymentSuccessfulScenarioHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class is responsible for catching payment event type, comparing it with existing
 * payment statuses and based on their correspondence, calling the desired scenario handler.
 * */

@Slf4j
@Service
public class PaymentEventCatcher {

    public void catchPaymentEventType(Event event, PaymentIntent paymentIntent) {
        log.debug("Catch payment event type method: input parameters: event: {}, paymentIntent: {}.", event, paymentIntent);
        if (Objects.nonNull(paymentIntent) && Objects.nonNull(event)) {
            log.info("Catch payment event type method: start of catching payment event type");

            Arrays.stream(PaymentStatus.values())
                    .filter(value -> Objects.equals(value.getStatus(), event.getType()))
                    .findAny()
                    .ifPresent(paymentStatus -> getPaymentHandlerByStatus(paymentStatus)
                            .handlePaymentScenario(paymentIntent));
        }
        log.info("Catch payment event type method: finish of catch payment event type");
    }

    private PaymentScenarioHandler getPaymentHandlerByStatus(PaymentStatus paymentStatus) {
        return switch (paymentStatus) {
            case PAYMENT_IS_SUCCEEDED -> new PaymentSuccessfulScenarioHandler();
            case PAYMENT_IS_FAILED -> new PaymentFailedScenarioHandler();
            default -> throw new UnexpectedPaymentStatusException(paymentStatus);
        };
    }

}
