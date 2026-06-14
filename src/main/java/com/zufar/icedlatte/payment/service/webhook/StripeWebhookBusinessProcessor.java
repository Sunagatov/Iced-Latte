package com.zufar.icedlatte.payment.service.webhook;

import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHARGE_REFUNDED;
import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED;
import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED;
import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHECKOUT_SESSION_COMPLETED;
import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHECKOUT_SESSION_EXPIRED;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.OrderStatusSnapshot;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.payment.service.PaymentConfirmationService;
import com.zufar.icedlatte.payment.service.PaymentConfirmationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional webhook business logic, extracted into a separate bean to ensure @Transactional is honored (avoids
 * Spring self-invocation trap).
 *
 * <p>Non-retryable business failures (e.g., amount mismatch) are persisted and the method returns normally — no throw
 * inside @Transactional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // Spring injects this bean; webhook flow enters through framework-managed calls.
public class StripeWebhookBusinessProcessor {

    private final OrderPaymentApi orderPaymentApi;
    private final PaymentRepository paymentRepository;
    private final PaymentConfirmationService paymentConfirmationService;

    @Transactional
    public void process(Event event) {
        String eventType = event.getType();
        if (isSessionCompletedEvent(eventType)) {
            handleSessionCompleted(event, requireSession(event));
            return;
        }

        switch (eventType) {
            case String type when CHECKOUT_SESSION_EXPIRED.matches(type) -> handleExpired(requireSession(event));
            case String type
            when CHECKOUT_SESSION_ASYNC_PAYMENT_FAILED.matches(type) -> handleAsyncPaymentFailed(requireSession(event));
            case String type when CHARGE_REFUNDED.matches(type) -> handleChargeRefunded(event);
            default -> log.debug("payment.webhook.unhandled: eventType={}", eventType);
        }
    }

    private boolean isSessionCompletedEvent(String eventType) {
        return CHECKOUT_SESSION_COMPLETED.matches(eventType)
                || CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED.matches(eventType);
    }

    private void handleSessionCompleted(Event event, Session stripeSession) {
        if (!"paid".equals(stripeSession.getPaymentStatus())) {
            UUID orderId = extractOrderId(stripeSession);
            Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
            if (payment == null || payment.getStatus().isTerminal()) {
                String logMessage = "payment.awaiting_async.skipped: orderId={}, status={}";
                log.info(logMessage, orderId, payment != null ? payment.getStatus() : "missing");
                return;
            }
            payment.setStatus(PaymentStatus.AWAITING_ASYNC_CONFIRMATION);
            payment.setRawEventId(event.getId());
            payment.setLatestEventType(event.getType());
            paymentRepository.save(payment);
            log.info("payment.awaiting_async: orderId={}, paymentStatus={}", orderId, stripeSession.getPaymentStatus());
            return;
        }
        UUID orderId = extractOrderId(stripeSession);
        paymentConfirmationService.confirmPaid(
                orderId,
                stripeSession,
                new PaymentConfirmationSource(event.getId(), event.getType(), "Stripe payment confirmed"));
    }

    private void handleExpired(Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null || payment.getStatus().isTerminal()) {
            String logMessage = "payment.expired.skipped: orderId={}, status={}";
            log.info(logMessage, orderId, payment != null ? payment.getStatus() : "missing");
            return;
        }

        payment.setStatus(PaymentStatus.EXPIRED);
        paymentRepository.save(payment);

        if (!orderPaymentApi.expirePayment(orderId, "Stripe session expired")) {
            log.warn("order.expire.transition_failed: orderId={}", orderId);
        }
    }

    private void handleAsyncPaymentFailed(Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null || payment.getStatus().isTerminal()) {
            String logMessage = "payment.async_failed.skipped: orderId={}, status={}";
            log.info(logMessage, orderId, payment != null ? payment.getStatus() : "missing");
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        if (!orderPaymentApi.failPayment(orderId, "Stripe async payment failed")) {
            log.warn("order.payment_failed.transition_failed: orderId={}", orderId);
        }
    }

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
        Optional<OrderSnapshot> orderOpt = orderPaymentApi.findByStripePaymentIntentId(paymentIntentId);

        if (orderOpt.isEmpty()) {
            log.warn("payment.webhook.order_not_found: paymentIntentId={}", paymentIntentId);
            return;
        }

        OrderSnapshot order = orderOpt.get();
        if (order.status() == OrderStatusSnapshot.REFUND_REQUESTED) {
            if (orderPaymentApi.confirmRefund(order.id(), "Stripe refund confirmed")) {
                paymentRepository.findByOrderIdForUpdate(order.id()).ifPresent(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setRawEventId(event.getId());
                    payment.setLatestEventType(event.getType());
                    paymentRepository.save(payment);
                });
                log.info("order.refund.confirmed: orderId={}, paymentIntentId={}", order.id(), paymentIntentId);
            } else {
                log.warn("order.refund.transition_failed: orderId={}, status={}", order.id(), order.status());
            }
        } else {
            log.info("order.refund.webhook_ignored: orderId={}, status={}", order.id(), order.status());
        }
    }

    private Session requireSession(Event event) {
        return event.getDataObjectDeserializer()
                .getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElseThrow(() -> {
                    String logMessage = "payment.webhook.session_missing: eventType={}, eventId={}";
                    log.warn(logMessage, event.getType(), event.getId());
                    return new IllegalStateException("Stripe webhook event session data is missing.");
                });
    }

    private UUID extractOrderId(Session session) {
        String orderId = session.getClientReferenceId();
        if (orderId == null) {
            orderId = session.getMetadata().get("orderId");
        }
        if (orderId == null) {
            throw new IllegalStateException("No orderId in Stripe session metadata");
        }
        return UUID.fromString(orderId);
    }
}
