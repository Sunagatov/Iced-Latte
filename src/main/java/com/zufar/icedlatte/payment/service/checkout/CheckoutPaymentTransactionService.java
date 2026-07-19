package com.zufar.icedlatte.payment.service.checkout;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.AddressDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.order.api.OrderCheckoutApi;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.api.dto.CheckoutOrderRequest;
import com.zufar.icedlatte.order.api.dto.OrderAddressRequest;
import com.zufar.icedlatte.payment.config.StripeProperties;
import com.zufar.icedlatte.payment.dto.CheckoutPaymentSnapshot;
import com.zufar.icedlatte.payment.dto.CheckoutPreparation;
import com.zufar.icedlatte.payment.dto.StripeSessionResult;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.entity.PaymentProvider;
import com.zufar.icedlatte.payment.entity.PaymentStatus;
import com.zufar.icedlatte.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Transactional methods for the checkout flow, extracted into a separate bean to avoid Spring self-invocation
 * on @Transactional (proxy-based AOP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // Spring injects this bean; transactional entry points are framework-managed.
public class CheckoutPaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final OrderPaymentApi orderPaymentApi;
    private final OrderCheckoutApi orderCheckoutApi;
    private final CartCheckoutApi cartCheckoutApi;
    private final StripeProperties stripeProperties;

    @Transactional
    public CheckoutPreparation prepareCheckout(UUID userId, CreateCheckoutRequestDto request, String idempotencyKey) {
        // Application-level idempotency: same user + same key → return existing
        Optional<CheckoutPreparation> existing = findExistingCheckout(userId, idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        CartSnapshot cart = cartCheckoutApi.getByUserIdOrThrow(userId);
        if (cart.items().isEmpty()) {
            throw new BadRequestException("Cannot checkout: shopping cart is empty");
        }

        CheckoutOrderRequest checkoutOrderRequest = toCheckoutOrderRequest(request);
        OrderSnapshot order = orderCheckoutApi.createPendingPaymentOrderSnapshot(userId, checkoutOrderRequest, cart);

        Payment payment = Payment.builder()
                .orderId(order.id())
                .userId(userId)
                .provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.CREATED)
                .amountMinor(toMinorUnits(order.itemsTotalPrice()))
                .currency(stripeProperties.currency())
                .checkoutIdempotencyKey(idempotencyKey)
                .build();
        payment = paymentRepository.saveAndFlush(payment);

        return new CheckoutPreparation.NewCheckout(order, toSnapshot(payment), cart.items());
    }

    @Transactional(readOnly = true)
    public Optional<CheckoutPreparation> findExistingCheckout(UUID userId, String idempotencyKey) {
        return paymentRepository
                .findByCheckoutIdempotencyKeyAndUserId(idempotencyKey, userId)
                .map(existing -> {
                    // Do NOT read the live cart — it may be deleted after successful payment.
                    // Use fetch join when Stripe session wasn't created yet — retry path needs Order.items.
                    OrderSnapshot order = (existing.getProviderSessionId() == null
                            ? orderPaymentApi.getSnapshotWithItems(existing.getOrderId())
                            : orderPaymentApi.getSnapshot(existing.getOrderId()));
                    log.info("checkout.idempotent_hit: userId={}, key={}", userId, idempotencyKey);
                    return new CheckoutPreparation.ExistingCheckout(order, toSnapshot(existing));
                });
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
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private CheckoutPaymentSnapshot toSnapshot(Payment payment) {
        return new CheckoutPaymentSnapshot(payment.getId(), payment.getProviderSessionId());
    }

    private static CheckoutOrderRequest toCheckoutOrderRequest(CreateCheckoutRequestDto request) {
        AddressDto address = request.getAddress();
        return new CheckoutOrderRequest(
                request.getRecipientName(),
                request.getRecipientSurname(),
                request.getRecipientPhone(),
                request.getDeliveryAddressId(),
                address == null ? null : toAddressRequest(address));
    }

    private static OrderAddressRequest toAddressRequest(AddressDto address) {
        return new OrderAddressRequest(
                requireAddressPart(address.getCountry(), "country"),
                requireAddressPart(address.getCity(), "city"),
                requireAddressPart(address.getLine(), "line"),
                requireAddressPart(address.getPostcode(), "postcode"));
    }

    private static String requireAddressPart(String value, String fieldName) {
        if (value == null) {
            throw new BadRequestException("Address field '" + fieldName + "' must be provided.");
        }
        return value;
    }
}
