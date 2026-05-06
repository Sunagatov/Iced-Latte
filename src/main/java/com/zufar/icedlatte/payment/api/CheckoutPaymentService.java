package com.zufar.icedlatte.payment.api;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.CheckoutResponseDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.config.StripeProperties;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Non-transactional coordinator for the checkout flow.
 * TX A (prepareCheckout) → Stripe API call → TX B (saveStripeDetails).
 * <p>
 * Iced Latte uses Stripe test mode only — no real money is charged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@SuppressWarnings("unused") // Spring injects this service and calls it from web entry points.
public class CheckoutPaymentService {

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final CheckoutPaymentTransactionService txService;
    private final StripeProperties stripeProperties;
    private final StripeCheckoutSessionCreator stripeSessionCreator;

    public CheckoutResponseDto checkout(CreateCheckoutRequestDto request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required and must not be blank.");
        }
        if (idempotencyKey.length() > 100) {
            throw new BadRequestException("Idempotency-Key must be at most 100 characters.");
        }

        UserDto user = securityPrincipalProvider.get();
        UUID userId = user.getId();

        // Stage 1: DB transaction — validate, create order + payment, commit
        CheckoutPreparation prepared = txService.prepareCheckout(userId, request, idempotencyKey);

        // Idempotent retry: don't call Stripe with empty line items
        if (prepared.existing()) {
            return resolveExistingCheckout(prepared, user.getEmail());
        }

        // Stage 2: Outside transaction — call Stripe
        StripeSessionResult stripeResult = stripeSessionCreator.create(
                prepared.order(), user.getEmail(), prepared.cartItems());

        // Stage 3: DB transaction — save Stripe details
        txService.saveStripeDetails(prepared.payment().getId(), stripeResult);

        log.info("checkout.created: orderId={}, stripeSessionId={}",
                prepared.order().getId(), stripeResult.sessionId());

        return new CheckoutResponseDto()
                .orderId(prepared.order().getId())
                .stripeSessionId(stripeResult.sessionId())
                .checkoutUrl(URI.create(stripeResult.checkoutUrl()));
    }

    /**
     * Handles idempotent retry. Session.retrieve() is a remote Stripe API call
     * and MUST remain outside any @Transactional method.
     */
    private CheckoutResponseDto resolveExistingCheckout(CheckoutPreparation prepared,
                                                        String customerEmail) {
        Payment payment = prepared.payment();

        // Case A: Stripe session already created — retrieve and return URL
        if (payment.getProviderSessionId() != null) {
            try {
                Session session = Session.retrieve(payment.getProviderSessionId());
                if ("expired".equals(session.getStatus())) {
                    throw new BadRequestException(
                            "Previous checkout session expired. Please retry with a new Idempotency-Key.");
                }
                return new CheckoutResponseDto()
                        .orderId(prepared.order().getId())
                        .stripeSessionId(payment.getProviderSessionId())
                        .checkoutUrl(URI.create(session.getUrl()));
            } catch (StripeException e) {
                throw new StripeSessionCreationException("Failed to retrieve existing session", e);
            }
        }

        // Case B: Order+Payment created but Stripe call failed — retry.
        // Rebuild line items from persisted Order.items (NOT the live cart).
        Order order = prepared.order();
        List<SessionCreateParams.LineItem> lineItems = order.getItems().stream()
                .map(this::toStripeLineItem)
                .toList();

        StripeSessionResult stripeResult = stripeSessionCreator.createFromLineItems(
                order, customerEmail, lineItems);

        txService.saveStripeDetails(payment.getId(), stripeResult);

        return new CheckoutResponseDto()
                .orderId(order.getId())
                .stripeSessionId(stripeResult.sessionId())
                .checkoutUrl(URI.create(stripeResult.checkoutUrl()));
    }

    private SessionCreateParams.LineItem toStripeLineItem(OrderItem item) {
        return SessionCreateParams.LineItem.builder()
                .setQuantity((long) item.getProductsQuantity())
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(stripeProperties.getCurrency())
                        .setUnitAmount(item.getProductPrice()
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(0, RoundingMode.UNNECESSARY)
                                .longValueExact())
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                .setName(item.getProductName())
                                .build())
                        .build())
                .build();
    }
}
