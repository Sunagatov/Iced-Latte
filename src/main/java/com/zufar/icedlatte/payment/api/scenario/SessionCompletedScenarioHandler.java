package com.zufar.icedlatte.payment.api.scenario;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.email.sender.PaymentEmailConfirmation;
import com.zufar.icedlatte.order.api.OrderCreator;
import com.zufar.icedlatte.payment.enums.StripeSessionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
@Slf4j
@RequiredArgsConstructor
@Service(StripeSessionConstants.SESSION_IS_COMPLETED)
public class SessionCompletedScenarioHandler implements SessionScenarioHandler {

    private final PaymentEmailConfirmation paymentEmailConfirmation;
    private final OrderCreator orderCreator;

    public void handle(Session stripeSession) {
        boolean orderWasCreated = orderCreator.createOrderAndDeleteCart(stripeSession);
        if (orderWasCreated) {
            paymentEmailConfirmation.send(stripeSession);
            log.info("payment.session.email.sent");
        } else {
            log.info("payment.session.already_processed: sessionId={}", stripeSession.getId());
        }
    }
}
