package com.zufar.icedlatte.payment.service;

import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.payment.entity.Payment;
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

    private final StripeSessionGateway stripeSessionGateway;
    private final PaymentConfirmationService paymentConfirmationService;

    public void trySyncPaidStatus(Payment payment) {
        try {
            Session session = stripeSessionGateway.retrieve(payment.getProviderSessionId());
            if ("paid".equals(session.getPaymentStatus())) {
                paymentConfirmationService.confirmPaid(
                        payment.getOrderId(),
                        session,
                        new PaymentConfirmationSource(
                                null, "sync.session.retrieve", "Stripe payment confirmed (sync fallback)"));
            }
        } catch (StripeException e) {
            log.warn("payment.sync.stripe_error: orderId={}, error={}", payment.getOrderId(), e.getMessage());
        }
    }
}
