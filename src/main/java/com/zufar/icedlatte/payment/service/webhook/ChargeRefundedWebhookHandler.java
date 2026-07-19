package com.zufar.icedlatte.payment.service.webhook;

import static com.zufar.icedlatte.payment.service.webhook.StripeWebhookEventType.CHARGE_REFUNDED;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.OrderStatusSnapshot;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class ChargeRefundedWebhookHandler implements StripeWebhookEventHandler {

    private final OrderPaymentApi orderPaymentApi;
    private final PaymentRepository paymentRepository;

    @Override
    public boolean supports(Event event) {
        return CHARGE_REFUNDED.matches(event.getType());
    }

    @Override
    public void handle(Event event) {
        String eventType = event.getType();
        var charge = event.getDataObjectDeserializer()
                .getObject()
                .filter(Charge.class::isInstance)
                .map(Charge.class::cast)
                .orElse(null);

        String eventId = event.getId();
        if (charge == null) {
            log.warn("payment.webhook.charge_missing: eventId={}", eventId);
            return;
        }

        String paymentIntentId = charge.getPaymentIntent();
        Optional<OrderSnapshot> orderOpt = orderPaymentApi.findByStripePaymentIntentId(paymentIntentId);

        if (orderOpt.isEmpty()) {
            log.warn("payment.webhook.order_not_found: paymentIntentId={}", paymentIntentId);
            return;
        }

        OrderSnapshot order = orderOpt.get();
        UUID orderId = order.id();
        if (order.status() != OrderStatusSnapshot.REFUND_REQUESTED) {
            log.info("order.refund.webhook_ignored: orderId={}, status={}", orderId, order.status());
            return;
        }
        if (!isFullyRefunded(charge)) {
            log.info(
                    "order.refund.webhook_partial_ignored: orderId={}, amount={}, amountRefunded={}",
                    orderId,
                    charge.getAmount(),
                    charge.getAmountRefunded());
            return;
        }
        if (!orderPaymentApi.confirmRefund(orderId, "Stripe refund confirmed")) {
            log.warn("order.refund.transition_failed: orderId={}, status={}", orderId, order.status());
            return;
        }
        paymentRepository.findByOrderIdForUpdate(orderId).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRawEventId(eventId);
            payment.setLatestEventType(eventType);

            paymentRepository.save(payment);
        });
        log.info("order.refund.confirmed: orderId={}, paymentIntentId={}", orderId, paymentIntentId);
    }

    private static boolean isFullyRefunded(Charge charge) {
        if (Boolean.TRUE.equals(charge.getRefunded())) {
            return true;
        }
        Long amount = charge.getAmount();
        Long amountRefunded = charge.getAmountRefunded();
        return amount != null && amountRefunded != null && amountRefunded >= amount;
    }
}
