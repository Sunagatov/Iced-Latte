package com.zufar.icedlatte.payment.api;

import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.openapi.dto.PaymentConfirmationEmail;
import com.zufar.icedlatte.payment.api.scenario.SessionCompletedScenarioHandler;
import com.zufar.icedlatte.payment.exception.StripeSessionIsNotComplete;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class has the same aim as WebhookEventProcessor,
 * but it's supposed to be used for processing request from frontend which is sent after
 * Stripe redirects to the specified URL.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RedirectEventProcessor {

    private static final String SESSION_COMPLETE = "complete";
    private final StripeSessionProvider stripeSessionProvider;
    private final SessionCompletedScenarioHandler sessionCompletedScenarioHandler;

    public PaymentConfirmationEmail processPaymentEvent(final String sessionId) {
        log.info("payment.redirect.processing");
        Session session = stripeSessionProvider.get(sessionId);
        String status = session.getStatus();
        if (!SESSION_COMPLETE.equals(status)) {
            log.warn("payment.redirect.session_not_complete: status={}", status);
            throw new StripeSessionIsNotComplete(sessionId, status);
        }
        sessionCompletedScenarioHandler.handle(session);
        log.info("payment.redirect.processed");
        var response = new PaymentConfirmationEmail();
        response.customerEmail(session.getCustomerEmail());
        return response;
    }
}
