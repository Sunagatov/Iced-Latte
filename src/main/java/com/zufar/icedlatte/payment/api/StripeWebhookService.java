package com.zufar.icedlatte.payment.api;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.zufar.icedlatte.email.sender.PaymentEmailConfirmation;
import com.zufar.icedlatte.openapi.dto.PaymentConfirmationEmail;
import com.zufar.icedlatte.order.api.OrderCreator;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.zufar.icedlatte.payment.exception.StripeSessionIsNotComplete;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class StripeWebhookService {

    private static final String SESSION_COMPLETE = "complete";

    private final OrderCreator orderCreator;
    private final PaymentEmailConfirmation paymentEmailConfirmation;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public void processWebhook(String payload, String stripeSignature) {
        log.info("payment.webhook.processing");
        Event event = parseEvent(payload, stripeSignature);
        Session session = extractSession(event);

        if (session == null) {
            log.warn("payment.webhook.no_session: eventType={}", event.getType());
            return;
        }

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCompleted(session);
            case "checkout.session.expired" -> log.info("payment.session.expired: sessionId={}", session.getId());
            default -> log.warn("payment.webhook.no_handler: eventType={}", event.getType());
        }

        log.info("payment.webhook.processed: eventType={}", event.getType());
    }

    public PaymentConfirmationEmail processRedirect(String sessionId) {
        log.info("payment.redirect.processing: sessionId={}", sessionId);
        Session session = retrieveSession(sessionId);
        if (!SESSION_COMPLETE.equals(session.getStatus())) {
            log.warn("payment.redirect.session_not_complete: status={}", session.getStatus());
            throw new StripeSessionIsNotComplete(sessionId, session.getStatus());
        }
        handleCompleted(session);
        return new PaymentConfirmationEmail().customerEmail(session.getCustomerEmail());
    }

    private void handleCompleted(Session session) {
        boolean created = orderCreator.createOrderAndDeleteCart(session);
        if (created) {
            paymentEmailConfirmation.send(session);
            log.info("payment.session.email.sent: sessionId={}", session.getId());
        } else {
            log.info("payment.session.already_processed: sessionId={}", session.getId());
        }
    }

    private Event parseEvent(String payload, String signature) {
        try {
            return Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("payment.webhook.signature_invalid");
            throw new PaymentEventProcessingException(signature);
        }
    }

    private Session extractSession(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElse(null);
    }

    private Session retrieveSession(String sessionId) {
        try {
            return Session.retrieve(sessionId);
        } catch (StripeException e) {
            throw new StripeSessionCreationException(e.getMessage(), e);
        }
    }
}
