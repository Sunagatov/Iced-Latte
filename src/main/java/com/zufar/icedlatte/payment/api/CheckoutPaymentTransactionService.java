package com.zufar.icedlatte.payment.api;

import com.zufar.icedlatte.cart.api.ShoppingCartService;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.order.api.OrderCreator;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.payment.config.StripeProperties;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentProvider;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Transactional methods for the checkout flow, extracted into a separate bean
 * to avoid Spring self-invocation on @Transactional (proxy-based AOP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // Spring injects this bean; transactional entry points are framework-managed.
public class CheckoutPaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderCreator orderCreator;
    private final ShoppingCartService shoppingCartService;
    private final StripeProperties stripeProperties;

    @Transactional
    public CheckoutPreparation prepareCheckout(UUID userId,
                                               CreateCheckoutRequestDto request,
                                               String idempotencyKey) {
        // Application-level idempotency: same user + same key → return existing
        Payment existing = paymentRepository
                .findByCheckoutIdempotencyKeyAndUserId(idempotencyKey, userId)
                .orElse(null);
        if (existing != null) {
            // Do NOT read the live cart — it may be deleted after successful payment.
            // Use fetch join when Stripe session wasn't created yet — retry path needs Order.items.
            Order order = (existing.getProviderSessionId() == null
                    ? orderRepository.findByIdWithItems(existing.getOrderId())
                    : orderRepository.findById(existing.getOrderId())
            ).orElseThrow();
            log.info("checkout.idempotent_hit: userId={}, key={}", userId, idempotencyKey);
            return new CheckoutPreparation(order, existing, List.of(), true);
        }

        ShoppingCartDto cart = shoppingCartService.getByUserIdOrThrow(userId);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout: shopping cart is empty");
        }

        Order order = orderCreator.createPendingPaymentOrder(userId, request, cart);

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .userId(userId)
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.CREATED)
                .amountMinor(toMinorUnits(order.getItemsTotalPrice()))
                .currency(stripeProperties.currency())
                .checkoutIdempotencyKey(idempotencyKey)
                .build();
        payment = paymentRepository.save(payment);

        return new CheckoutPreparation(order, payment, cart.getItems(), false);
    }

    @Transactional
    public void saveStripeDetails(UUID paymentId, StripeSessionResult stripeResult) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        payment.setProviderSessionId(stripeResult.sessionId());
        payment.setStatus(PaymentStatus.STRIPE_SESSION_CREATED);
        paymentRepository.save(payment);
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

}
