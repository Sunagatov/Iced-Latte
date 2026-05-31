package com.zufar.icedlatte.payment.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.payment.service.checkout.StripeSessionGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reconciles local payment state from Stripe when the webhook has not arrived yet. The webhook remains the primary
 * source of updates; this is the status-polling fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PaymentReconciliationService {

    private final OrderPaymentApi orderPaymentApi;
    private final PaymentRepository paymentRepository;
    private final CartCheckoutApi cartCheckoutApi;
    private final TransactionTemplate transactionTemplate;
    private final StripeSessionGateway stripeSessionGateway;

    public void trySyncPaidStatus(Payment payment) {
        try {
            Session session = stripeSessionGateway.retrieve(payment.getProviderSessionId());
            if ("paid".equals(session.getPaymentStatus())) {
                syncPaidStatus(payment.getOrderId(), session);
            }
        } catch (StripeException e) {
            log.warn("payment.sync.stripe_error: orderId={}, error={}", payment.getOrderId(), e.getMessage());
        }
    }

    private void syncPaidStatus(UUID orderId, Session session) {
        transactionTemplate.executeWithoutResult(status -> {
            Payment locked = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
            if (locked == null || locked.getStatus().isTerminal()) {
                return;
            }

            Long stripeAmount = session.getAmountTotal();
            String stripeCurrency = session.getCurrency();
            String paymentIntent = session.getPaymentIntent();
            if (stripeAmount == null
                    || stripeCurrency == null
                    || paymentIntent == null
                    || !stripeAmount.equals(locked.getAmountMinor())
                    || !stripeCurrency.equalsIgnoreCase(locked.getCurrency())) {
                log.error("payment.sync.reconciliation_failed: orderId={}", orderId);
                locked.setStatus(PaymentStatus.RECONCILIATION_FAILED);
                locked.setLatestEventType("sync.session.retrieve");
                paymentRepository.save(locked);
                return;
            }

            locked.setProviderPaymentIntentId(paymentIntent);
            locked.setStatus(PaymentStatus.PAID);
            locked.setLatestEventType("sync.session.retrieve");
            paymentRepository.save(locked);

            if (orderPaymentApi.confirmPayment(orderId, "Stripe payment confirmed (sync fallback)")) {
                orderPaymentApi.assignPaymentIntent(orderId, paymentIntent);
                cartCheckoutApi.deleteCartForUser(locked.getUserId());
                log.info("payment.sync.confirmed: orderId={}, paymentIntentId={}", orderId, paymentIntent);
            } else {
                log.warn("payment.sync.order_transition_failed: orderId={}", orderId);
            }
        });
    }
}
