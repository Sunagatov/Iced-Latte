package com.zufar.icedlatte.payment.api;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.openapi.dto.CheckoutStatusDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.order.api.OrderStatusTransitioner;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.api.OrderDetailProvider;
import com.zufar.icedlatte.order.api.OrderLifecycleService;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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

    private final OrderDetailProvider orderDetailProvider;
    private final OrderLifecycleService orderLifecycleService;
    private final PaymentRepository paymentRepository;
    private final OrderStatusTransitioner orderStatusTransitioner;
    private final ShoppingCartService shoppingCartService;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final TransactionTemplate transactionTemplate;

    public CheckoutStatusDto getStatus(UUID orderId) {
        OrderSnapshot order = orderDetailProvider.getSnapshot(orderId);

        UserDto currentUser = securityPrincipalProvider.get();
        if (!order.userId().equals(currentUser.getId())) {
            throw new OrderAccessDeniedException();
        }

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        // Fallback: if webhook hasn't arrived yet, check Stripe directly.
        if (payment != null && !payment.getStatus().isTerminal()
                && payment.getProviderSessionId() != null) {
            trySyncFromStripe(payment);
            // Re-read after potential update
            order = orderDetailProvider.getSnapshot(orderId);
            payment = paymentRepository.findByOrderId(orderId).orElse(payment);
        }

        CheckoutStatusDto dto = new CheckoutStatusDto()
                .orderId(order.id())
                .orderStatus(order.status());

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
                syncPaidStatus(payment.getOrderId(), session);
            }
        } catch (StripeException e) {
            log.warn("payment.sync.stripe_error: orderId={}, error={}",
                    payment.getOrderId(), e.getMessage());
        }
    }

    /**
     * Updates payment/order to PAID inside a programmatic transaction.
     * Uses TransactionTemplate instead of @Transactional to avoid the
     * Spring self-invocation trap (calling a @Transactional method from
     * within the same class bypasses the proxy).
     */
    private void syncPaidStatus(UUID orderId, Session session) {
        transactionTemplate.executeWithoutResult(status -> {
            Payment locked = paymentRepository.findByOrderIdForUpdate(orderId).orElse(null);
            if (locked == null || locked.getStatus().isTerminal()) {
                return;
            }

            // Reconciliation guard: verify amount/currency
            Long stripeAmount = session.getAmountTotal();
            String stripeCurrency = session.getCurrency();
            if (stripeAmount != null && stripeCurrency != null) {
                if (!stripeAmount.equals(locked.getAmountMinor())
                        || !stripeCurrency.equalsIgnoreCase(locked.getCurrency())) {
                    log.error("payment.sync.amount_mismatch: orderId={}", orderId);
                    locked.setStatus(PaymentStatus.RECONCILIATION_FAILED);
                    paymentRepository.save(locked);
                    return;
                }
            }

            locked.setProviderPaymentIntentId(session.getPaymentIntent());
            locked.setStatus(PaymentStatus.PAID);
            locked.setLatestEventType("sync.session.retrieve");
            paymentRepository.save(locked);

            orderStatusTransitioner.transition(
                    orderId, OrderEvent.PENDING_PAYMENT_CONFIRMED,
                    null, "Stripe payment confirmed (sync fallback)");
            orderLifecycleService.assignPaymentIntent(orderId, session.getPaymentIntent());

            shoppingCartService.deleteCartForUser(locked.getUserId());

            log.info("payment.sync.confirmed: orderId={}, paymentIntentId={}",
                    orderId, session.getPaymentIntent());
        });
    }
}
