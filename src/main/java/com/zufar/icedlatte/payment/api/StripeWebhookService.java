package com.zufar.icedlatte.payment.api;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.zufar.icedlatte.common.util.SessionIdMasker;
import com.zufar.icedlatte.email.sender.PaymentEmailConfirmation;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.PaymentConfirmationEmail;
import com.zufar.icedlatte.order.api.OrderCreator;
import com.zufar.icedlatte.order.api.OrderStatusTransitioner;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.payment.exception.PaymentEventProcessingException;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.zufar.icedlatte.payment.exception.StripeSessionIsNotComplete;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripeWebhookService {

    private static final String SESSION_COMPLETE = "complete";

    private final OrderCreator orderCreator;
    private final PaymentEmailConfirmation paymentEmailConfirmation;
    private final OrderRepository orderRepository;
    private final OrderStatusTransitioner orderStatusTransitioner;

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
                log.info("payment.session.expired: sessionId={}", SessionIdMasker.mask(session.getId()));
                log.info("payment.webhook.processed: eventType={}, eventId={}", event.getType(), event.getId());
            }
            case "charge.refunded" -> {
                handleChargeRefunded(event);
                log.info("payment.webhook.processed: eventType={}, eventId={}", event.getType(), event.getId());
            }
            default -> log.debug("payment.webhook.unhandled: eventType={}, eventId={}", event.getType(), event.getId());
        }
    }

    public PaymentConfirmationEmail processRedirect(String sessionId) {
        log.debug("payment.redirect.processing: sessionId={}", SessionIdMasker.mask(sessionId));
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
            log.info("payment.email_confirmation.sent: sessionId={}", SessionIdMasker.mask(session.getId()));
        } else {
            log.info("payment.session.already_processed: sessionId={}", SessionIdMasker.mask(session.getId()));
        }
    }

    /**
     * Gap #6: Handle charge.refunded webhook by looking up order via stripe_payment_intent_id.
     * Transitions REFUND_REQUESTED → REFUNDED.
     */
    private void handleChargeRefunded(Event event) {
        var charge = event.getDataObjectDeserializer()
                .getObject()
                .filter(com.stripe.model.Charge.class::isInstance)
                .map(com.stripe.model.Charge.class::cast)
                .orElse(null);

        if (charge == null) {
            log.warn("payment.webhook.charge_missing: eventId={}", event.getId());
            return;
        }

        String paymentIntentId = charge.getPaymentIntent();
        Optional<Order> orderOpt = orderRepository.findByStripePaymentIntentId(paymentIntentId);

        if (orderOpt.isEmpty()) {
            log.warn("payment.webhook.order_not_found: paymentIntentId={}", paymentIntentId);
            return;
        }

        Order order = orderOpt.get();
        if (order.getStatus() == OrderStatus.REFUND_REQUESTED) {
            try {
                orderStatusTransitioner.transition(order.getId(), OrderEvent.REFUND_CONFIRMED, null, "Stripe refund confirmed");
                log.info("order.refund.confirmed: orderId={}, paymentIntentId={}", order.getId(), paymentIntentId);
            } catch (InvalidOrderStateTransitionException e) {
                log.warn("order.refund.transition_failed: orderId={}, status={}", order.getId(), order.getStatus());
            }
        } else {
            log.info("order.refund.webhook_ignored: orderId={}, status={}", order.getId(), order.getStatus());
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
}
