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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeWebhookService {

    private static final String SESSION_COMPLETE = "complete";

    private final OrderCreator orderCreator;
    private final PaymentEmailConfirmation paymentEmailConfirmation;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public void processWebhook(String payload, String stripeSignature) {
        Event event = parseEvent(payload, stripeSignature);

        switch (event.getType()) {
            case "checkout.session.completed" -> {
                handleCompleted(requireSession(event));
                log.info("payment.webhook.processed: eventType={}, eventId={}", event.getType(), event.getId());
            }
            case "checkout.session.expired" -> {
                Session session = requireSession(event);
                log.info("payment.session.expired: sessionId={}", maskSessionId(session.getId()));
                log.info("payment.webhook.processed: eventType={}, eventId={}", event.getType(), event.getId());
            }
            default -> log.debug("payment.webhook.unhandled: eventType={}, eventId={}", event.getType(), event.getId());
        }
    }

    public PaymentConfirmationEmail processRedirect(String sessionId) {
        log.debug("payment.redirect.processing: sessionId={}", maskSessionId(sessionId));
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
            log.info("payment.email_confirmation.sent: sessionId={}", maskSessionId(session.getId()));
        } else {
            log.info("payment.session.already_processed: sessionId={}", maskSessionId(session.getId()));
        }
    }

    private Event parseEvent(String payload, String signature) {
        try {
            return Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("payment.webhook.signature_invalid");
            throw new PaymentEventProcessingException();
        }
    }

    private Session requireSession(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElseThrow(() -> {
                    log.warn("payment.webhook.session_missing: eventType={}, eventId={}", event.getType(), event.getId());
                    return new IllegalStateException("Stripe webhook event session data is missing.");
                });
    }

    private Session retrieveSession(String sessionId) {
        try {
            return Session.retrieve(sessionId);
        } catch (StripeException e) {
            throw new StripeSessionCreationException(e.getMessage(), e);
        }
    }

    private static String maskSessionId(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return "unknown";
        }
        return StringUtils.left(StringUtils.overlay(sessionId, "****", 6, sessionId.length()), 10);
    }
}
