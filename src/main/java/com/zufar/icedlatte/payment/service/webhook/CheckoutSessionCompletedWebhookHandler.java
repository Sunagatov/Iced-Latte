package com.zufar.icedlatte.payment.service.webhook;

import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventObjects.extractOrderId;
import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventObjects.requireSession;
import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED;
import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHECKOUT_SESSION_COMPLETED;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.payment.service.PaymentConfirmationService;
import com.zufar.icedlatte.payment.service.PaymentConfirmationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class CheckoutSessionCompletedWebhookHandler implements StripeWebhookEventHandler {

    private final PaymentRepository paymentRepository;
    private final PaymentConfirmationService paymentConfirmationService;

    @Override
    public boolean supports(Event event) {
        String eventType = event.getType();
        return CHECKOUT_SESSION_COMPLETED.matches(eventType)
                || CHECKOUT_SESSION_ASYNC_PAYMENT_SUCCEEDED.matches(eventType);
    }

    @Override
    public void handle(Event event) {
        Session stripeSession = requireSession(event);
        String eventType = event.getType();
        String sessionPaymentStatus = stripeSession.getPaymentStatus();
        UUID orderId = extractOrderId(stripeSession);

        if ("paid".equals(sessionPaymentStatus)) {
            PaymentConfirmationSource stripePaymentConfirmed =
                    new PaymentConfirmationSource(event.getId(), eventType, "Stripe payment confirmed");
            paymentConfirmationService.confirmPaid(orderId, stripeSession, stripePaymentConfirmed);
            return;
        }
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (payment == null || payment.getStatus().isTerminal()) {
            String logMessage = "payment.awaiting_async.skipped: orderId={}, status={}";
            log.info(logMessage, orderId, paymentStatusOrMissing(payment));
            return;
        }
        payment.setStatus(PaymentStatus.AWAITING_ASYNC_CONFIRMATION);
        payment.setRawEventId(event.getId());
        payment.setLatestEventType(eventType);

        paymentRepository.save(payment);

        log.info("payment.awaiting_async: orderId={}, paymentStatus={}", orderId, sessionPaymentStatus);
    }

    private static String paymentStatusOrMissing(Payment payment) {
        return payment != null ? payment.getStatus().name() : "missing";
    }
}
