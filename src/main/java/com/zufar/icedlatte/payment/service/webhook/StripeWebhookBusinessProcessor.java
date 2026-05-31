package com.zufar.icedlatte.payment.service.webhook;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.OrderStatusSnapshot;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;

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
    private final CartCheckoutApi cartCheckoutApi;

    @Transactional
    public void process(Event event) {
        String eventType = event.getType();
        if (isSessionCompletedEvent(eventType)) {
            handleSessionCompleted(event, requireSession(event));
            return;
        }

        switch (eventType) {
            case "checkout.session.expired" -> handleExpired(requireSession(event));
            case "checkout.session.async_payment_failed" -> handleAsyncPaymentFailed(requireSession(event));
            case "charge.refunded" -> handleChargeRefunded(event);
            default -> log.debug("payment.webhook.unhandled: eventType={}", eventType);
        }
    }

    private boolean isSessionCompletedEvent(String eventType) {
        return "checkout.session.completed".equals(eventType)
                || "checkout.session.async_payment_succeeded".equals(eventType);
    }

    private void handleSessionCompleted(Event event, Session stripeSession) {
        if (!"paid".equals(stripeSession.getPaymentStatus())) {
            UUID orderId = extractOrderId(stripeSession);
            Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
            if (payment == null || payment.getStatus().isTerminal()) {
                log.info(
                        "payment.awaiting_async.skipped: orderId={}, status={}",
                        orderId,
                        payment != null ? payment.getStatus() : "missing");
                return;
            }
            payment.setStatus(PaymentStatus.AWAITING_ASYNC_CONFIRMATION);
            payment.setRawEventId(event.getId());
            payment.setLatestEventType(event.getType());
            paymentRepository.save(payment);
            log.info("payment.awaiting_async: orderId={}, paymentStatus={}", orderId, stripeSession.getPaymentStatus());
            return;
        }
        markPaid(event, stripeSession);
    }

    private void markPaid(Event event, Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        // PESSIMISTIC_WRITE lock — prevents concurrent processing of different events
        Payment payment = paymentRepository
                .findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException("No Payment for orderId=" + orderId));

        if (payment.getStatus().isTerminal()) {
            log.info("payment.paid.skipped_terminal: orderId={}, status={}", orderId, payment.getStatus());
            return;
        }

        // Reconciliation guard: verify amount/currency match.
        // On mismatch: persist RECONCILIATION_FAILED and return normally (no throw).
        Long stripeAmount = stripeSession.getAmountTotal();
        String stripeCurrency = stripeSession.getCurrency();
        String paymentIntent = stripeSession.getPaymentIntent();
        if (stripeAmount == null
                || stripeCurrency == null
                || paymentIntent == null
                || !stripeAmount.equals(payment.getAmountMinor())
                || !stripeCurrency.equalsIgnoreCase(payment.getCurrency())) {
            log.error(
                    "payment.reconciliation_failed: orderId={}, expected={}_{}, stripe={}_{}, paymentIntentPresent={}",
                    orderId,
                    payment.getAmountMinor(),
                    payment.getCurrency(),
                    stripeAmount,
                    stripeCurrency,
                    paymentIntent != null);
            payment.setStatus(PaymentStatus.RECONCILIATION_FAILED);
            payment.setRawEventId(event.getId());
            payment.setLatestEventType(event.getType());
            paymentRepository.save(payment);
            return; // TX commits — RECONCILIATION_FAILED is persisted
        }

        payment.setProviderPaymentIntentId(paymentIntent);
        payment.setRawEventId(event.getId());
        payment.setLatestEventType(event.getType());

        if (!orderPaymentApi.confirmPayment(orderId, "Stripe payment confirmed")) {
            payment.setStatus(PaymentStatus.RECONCILIATION_FAILED);
            paymentRepository.save(payment);
            log.warn("checkout.completed.order_transition_failed: orderId={}", orderId);
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        // Store stripePaymentIntentId on Order for refund lookup
        orderPaymentApi.assignPaymentIntent(orderId, paymentIntent);
        cartCheckoutApi.deleteCartForUser(payment.getUserId());
        log.info("checkout.completed: orderId={}, paymentIntentId={}", orderId, paymentIntent);
    }

    private void handleExpired(Session stripeSession) {
        UUID orderId = extractOrderId(stripeSession);

        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null || payment.getStatus().isTerminal()) {
            log.info(
                    "payment.expired.skipped: orderId={}, status={}",
                    orderId,
                    payment != null ? payment.getStatus() : "missing");
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
            log.info(
                    "payment.async_failed.skipped: orderId={}, status={}",
                    orderId,
                    payment != null ? payment.getStatus() : "missing");
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
                    log.warn(
                            "payment.webhook.session_missing: eventType={}, eventId={}",
                            event.getType(),
                            event.getId());
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
