package com.zufar.icedlatte.payment.service.checkout;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.common.exception.BadRequestException;
import com.zufar.icedlatte.common.turnstile.TurnstileProperties;
import com.zufar.icedlatte.common.turnstile.TurnstileVerifier;
import com.zufar.icedlatte.openapi.dto.CheckoutResponseDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.payment.converter.StripeSessionLineItemListConverter;
import com.zufar.icedlatte.payment.dto.CheckoutPaymentSnapshot;
import com.zufar.icedlatte.payment.dto.CheckoutPreparation;
import com.zufar.icedlatte.payment.dto.StripeSessionResult;
import com.zufar.icedlatte.payment.exception.StripeSessionException;
import com.zufar.icedlatte.security.api.CurrentUserProvider;
import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Non-transactional coordinator for the checkout flow. TX A (prepareCheckout) → Stripe API call → TX B
 * (saveStripeDetails).
 *
 * <p>Iced Latte uses Stripe test mode only — no real money is charged.
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
    private final StripeSessionGateway stripeSessionGateway;
    private final StripeSessionLineItemListConverter lineItemConverter;
    private final TurnstileVerifier turnstileVerifier;
    private final TurnstileProperties turnstileProperties;

    public CheckoutResponseDto checkout(CreateCheckoutRequestDto request, String idempotencyKey) {
        return checkout(request, idempotencyKey, null);
    }

    public CheckoutResponseDto checkout(CreateCheckoutRequestDto request, String idempotencyKey, String remoteIp) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required and must not be blank.");
        }
        if (idempotencyKey.length() > 100) {
            throw new BadRequestException("Idempotency-Key must be at most 100 characters.");
        }
        if (turnstileProperties.checkoutEnabled()) {
            turnstileVerifier.verify(request.getTurnstileToken(), remoteIp);
        }

        CurrentUserSnapshot user = currentUserProvider.get();
        UUID userId = user.id();
        String customerEmail = user.email();

        // Stage 1: DB transaction — validate, create order + payment, commit
        CheckoutPreparation prepared = prepareCheckout(userId, request, idempotencyKey);

        // Idempotent retry: don't call Stripe with empty line items
        return switch (prepared) {
            case CheckoutPreparation.ExistingCheckout existingCheckout ->
                resolveExistingCheckout(existingCheckout, customerEmail);
            case CheckoutPreparation.NewCheckout newCheckout -> {
                // Stage 2: Outside transaction — call Stripe
                OrderSnapshot orderSnapshot = newCheckout.order();
                StripeSessionResult stripeResult =
                        stripeSessionCreator.create(orderSnapshot, customerEmail, newCheckout.cartItems());

                // Stage 3: DB transaction — save Stripe details
                txService.saveStripeDetails(newCheckout.payment().id(), stripeResult);

                log.info(
                        "checkout.created: orderId={}, stripeSessionId={}",
                        orderSnapshot.id(),
                        stripeResult.sessionId());

                yield new CheckoutResponseDto()
                        .orderId(orderSnapshot.id())
                        .stripeSessionId(stripeResult.sessionId())
                        .checkoutUrl(URI.create(stripeResult.checkoutUrl()));
            }
        };
    }

    private CheckoutPreparation prepareCheckout(UUID userId, CreateCheckoutRequestDto request, String idempotencyKey) {
        try {
            return txService.prepareCheckout(userId, request, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            log.info("checkout.idempotency_collision: userId={}, key={}", userId, idempotencyKey);
            return txService.findExistingCheckout(userId, idempotencyKey).orElseThrow(() -> e);
        }
    }

    /**
     * Handles idempotent retry. Session.retrieve() is a remote Stripe API call and MUST remain outside
     * any @Transactional method.
     */
    private CheckoutResponseDto resolveExistingCheckout(
            CheckoutPreparation.ExistingCheckout prepared, String customerEmail) {
        CheckoutPaymentSnapshot payment = prepared.payment();

        // Case A: Stripe session already created — retrieve and return URL
        if (payment.providerSessionId() != null) {
            try {
                Session session = stripeSessionGateway.retrieve(payment.providerSessionId());
                if ("expired".equals(session.getStatus())) {
                    throw new BadRequestException(
                            "Previous checkout session expired. Please retry with a new Idempotency-Key.");
                }
                return new CheckoutResponseDto()
                        .orderId(prepared.order().id())
                        .stripeSessionId(payment.providerSessionId())
                        .checkoutUrl(URI.create(session.getUrl()));
            } catch (StripeException e) {
                throw new StripeSessionException("Failed to retrieve existing session", e);
            }
        }

        // Case B: Order+Payment created but Stripe call failed — retry.
        // Rebuild line items from persisted Order.items (NOT the live cart).
        OrderSnapshot order = prepared.order();
        List<SessionCreateParams.LineItem> lineItems =
                order.items().stream().map(lineItemConverter::toLineItem).toList();

        StripeSessionResult stripeResult = stripeSessionCreator.createFromLineItems(order, customerEmail, lineItems);

        txService.saveStripeDetails(payment.id(), stripeResult);

        return new CheckoutResponseDto()
                .orderId(order.id())
                .stripeSessionId(stripeResult.sessionId())
                .checkoutUrl(URI.create(stripeResult.checkoutUrl()));
    }
}
