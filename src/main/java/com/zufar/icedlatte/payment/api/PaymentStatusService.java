package com.zufar.icedlatte.payment.api;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.CheckoutStatusDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.order.api.OrderStatusTransitioner;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Status polling for the success page.
 * <p>
 * Primary path: the Stripe webhook updates payment/order status before the
 * success page polls. This is how real payment systems work.
 * <p>
 * Fallback path: if the webhook hasn't arrived yet (common in local dev
 * without Stripe CLI), this service calls Session.retrieve() directly and
 * updates the status. Real payment systems always have a reconciliation
 * fallback — never rely on a single delivery mechanism for money.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class PaymentStatusService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusTransitioner orderStatusTransitioner;
    private final ShoppingCartRepository shoppingCartRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    public CheckoutStatusDto getStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        UserDto currentUser = securityPrincipalProvider.get();
        if (!order.getUserId().equals(currentUser.getId())) {
            throw new OrderAccessDeniedException(orderId);
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        // Fallback: if webhook hasn't arrived yet, check Stripe directly.
        if (payment != null && !payment.getStatus().isTerminal()
                && payment.getProviderSessionId() != null) {
            trySyncFromStripe(payment);
            // Re-read order after potential update
            order = orderRepository.findById(orderId).orElseThrow();
            payment = paymentRepository.findByOrderId(orderId).orElse(payment);
        }

        CheckoutStatusDto dto = new CheckoutStatusDto()
                .orderId(order.getId())
                .orderStatus(order.getStatus());

        if (payment != null) {
            dto.paymentStatus(CheckoutStatusDto.PaymentStatusEnum.fromValue(payment.getStatus().name()));
        }

        return dto;
    }

    /**
     * Calls Stripe Session.retrieve() and updates local status if Stripe
     * confirms payment. This is the reconciliation fallback — the webhook
     * is the primary path.
     */
    private void trySyncFromStripe(Payment payment) {
        try {
            Session session = Session.retrieve(payment.getProviderSessionId());
            if ("paid".equals(session.getPaymentStatus())) {
                syncPaidStatus(payment, session);
            }
        } catch (StripeException e) {
            log.warn("payment.sync.stripe_error: orderId={}, error={}",
                    payment.getOrderId(), e.getMessage());
        }
    }

    @Transactional
    protected void syncPaidStatus(Payment payment, Session session) {
        // Re-read with lock to prevent race with webhook
        Payment locked = paymentRepository.findByOrderIdForUpdate(payment.getOrderId())
                .orElse(null);
        if (locked == null || locked.getStatus().isTerminal()) {
            return;
        }

        // Reconciliation guard: verify amount/currency
        Long stripeAmount = session.getAmountTotal();
        String stripeCurrency = session.getCurrency();
        if (stripeAmount != null && stripeCurrency != null) {
            if (!stripeAmount.equals(locked.getAmountMinor())
                    || !stripeCurrency.equalsIgnoreCase(locked.getCurrency())) {
                log.error("payment.sync.amount_mismatch: orderId={}", locked.getOrderId());
                locked.setStatus(PaymentStatus.RECONCILIATION_FAILED);
                paymentRepository.save(locked);
                return;
            }
        }

        locked.setProviderPaymentIntentId(session.getPaymentIntent());
        locked.setStatus(PaymentStatus.PAID);
        locked.setLatestEventType("sync.session.retrieve");
        paymentRepository.save(locked);

        Order order = orderStatusTransitioner.transition(
                locked.getOrderId(), OrderEvent.PENDING_PAYMENT_CONFIRMED,
                null, "Stripe payment confirmed (sync fallback)");
        order.setStripePaymentIntentId(session.getPaymentIntent());
        orderRepository.save(order);

        shoppingCartRepository.deleteByUserId(order.getUserId());

        log.info("payment.sync.confirmed: orderId={}, paymentIntentId={}",
                locked.getOrderId(), session.getPaymentIntent());
    }
}
