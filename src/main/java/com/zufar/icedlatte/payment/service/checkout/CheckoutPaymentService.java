package com.zufar.icedlatte.payment.service.checkout;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.openapi.dto.CheckoutResponseDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.payment.converter.StripeSessionLineItemListConverter;
import com.zufar.icedlatte.payment.dto.CheckoutPreparation;
import com.zufar.icedlatte.payment.dto.StripeSessionResult;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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

    private final CurrentUserProvider currentUserProvider;
    private final CheckoutPaymentTransactionService txService;
    private final StripeCheckoutSessionCreator stripeSessionCreator;
    private final StripeSessionLineItemListConverter lineItemConverter;

    public CheckoutResponseDto checkout(CreateCheckoutRequestDto request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required and must not be blank.");
        }
        if (idempotencyKey.length() > 100) {
            throw new BadRequestException("Idempotency-Key must be at most 100 characters.");
        }

        CurrentUserSnapshot user = currentUserProvider.get();
        UUID userId = user.id();

        // Stage 1: DB transaction — validate, create order + payment, commit
        CheckoutPreparation prepared = txService.prepareCheckout(userId, request, idempotencyKey);

        // Idempotent retry: don't call Stripe with empty line items
        if (prepared.existing()) {
            return resolveExistingCheckout(prepared, user.email());
        }

        // Stage 2: Outside transaction — call Stripe
        StripeSessionResult stripeResult = stripeSessionCreator.create(
                prepared.order(), user.email(), prepared.cartItems());

        // Stage 3: DB transaction — save Stripe details
        txService.saveStripeDetails(prepared.payment().getId(), stripeResult);

        log.info("checkout.created: orderId={}, stripeSessionId={}",
                prepared.order().id(), stripeResult.sessionId());

        return new CheckoutResponseDto()
                .orderId(prepared.order().id())
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
                        .orderId(prepared.order().id())
                        .stripeSessionId(payment.getProviderSessionId())
                        .checkoutUrl(URI.create(session.getUrl()));
            } catch (StripeException e) {
                throw new StripeSessionCreationException("Failed to retrieve existing session", e);
            }
        }

        // Case B: Order+Payment created but Stripe call failed — retry.
        // Rebuild line items from persisted Order.items (NOT the live cart).
        OrderSnapshot order = prepared.order();
        List<SessionCreateParams.LineItem> lineItems = order.items().stream()
                .map(lineItemConverter::toLineItem)
                .toList();

        StripeSessionResult stripeResult = stripeSessionCreator.createFromLineItems(
                order, customerEmail, lineItems);

        txService.saveStripeDetails(payment.getId(), stripeResult);

        return new CheckoutResponseDto()
                .orderId(order.id())
                .stripeSessionId(stripeResult.sessionId())
                .checkoutUrl(URI.create(stripeResult.checkoutUrl()));
    }

}
