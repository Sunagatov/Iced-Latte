package com.zufar.icedlatte.payment.api;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.payment.config.StripeProperties;
import com.zufar.icedlatte.payment.converter.StripeSessionLineItemListConverter;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Creates Stripe Hosted Checkout Sessions (test mode only — no real money).
 * Receives a persisted Order, not an HttpServletRequest.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@EnableConfigurationProperties(StripeProperties.class)
@SuppressWarnings("unused") // Spring manages bean lifecycle and injects configuration fields.
public class StripeCheckoutSessionCreator {

    private final StripeSessionLineItemListConverter lineItemConverter;
    private final StripeProperties stripeProperties;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${frontend.url}")
    private String frontendUrl;

    @PostConstruct
    private void initStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Cart-based entry point (normal checkout). Converts cart items to Stripe
     * line items, then delegates to {@link #createFromLineItems}.
     */
    public StripeSessionResult create(Order order, String customerEmail,
                                      List<ShoppingCartItemDto> cartItems) {
        List<SessionCreateParams.LineItem> lineItems = lineItemConverter.toLineItems(cartItems);
        return createFromLineItems(order, customerEmail, lineItems);
    }

    /**
     * Core method used by both normal checkout and idempotent retry.
     * Accepts pre-built Stripe line items so the retry path can rebuild them
     * from the persisted Order.items snapshot without needing the original cart.
     */
    public StripeSessionResult createFromLineItems(Order order, String customerEmail,
                                                   List<SessionCreateParams.LineItem> lineItems) {
        String orderId = order.getId().toString();
        String userId = order.getUserId().toString();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomerEmail(customerEmail)
                .setSuccessUrl(frontendUrl + "/checkout/success?session_id={CHECKOUT_SESSION_ID}&order_id=" + orderId)
                .setCancelUrl(frontendUrl + "/checkout/cancel?order_id=" + orderId)
                .setClientReferenceId(orderId)
                .putMetadata("orderId", orderId)
                .putMetadata("userId", userId)
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("orderId", orderId)
                                .putMetadata("userId", userId)
                                .build())
                .addAllLineItem(lineItems)
                .addAllShippingOption(shippingOptions())
                .setExpiresAt(OffsetDateTime.now().plusMinutes(31).toEpochSecond())
                .build();

        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey("checkout-session:" + orderId)
                .build();

        try {
            Session session = Session.create(params, requestOptions);
            return new StripeSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new StripeSessionCreationException(e.getMessage(), e);
        }
    }

    private List<SessionCreateParams.ShippingOption> shippingOptions() {
        return stripeProperties.shippingOptions().stream()
                .map(opt -> buildShippingOption(opt.name(), opt.amountCents(), opt.minDays(), opt.maxDays()))
                .toList();
    }

    private SessionCreateParams.ShippingOption buildShippingOption(String name,
                                                                   long amountCents,
                                                                   long minDays,
                                                                   long maxDays) {
        return SessionCreateParams.ShippingOption.builder()
                .setShippingRateData(
                        SessionCreateParams.ShippingOption.ShippingRateData.builder()
                                .setType(SessionCreateParams.ShippingOption.ShippingRateData.Type.FIXED_AMOUNT)
                                .setFixedAmount(SessionCreateParams.ShippingOption.ShippingRateData.FixedAmount.builder()
                                        .setAmount(amountCents)
                                        .setCurrency(stripeProperties.currency())
                                        .build())
                                .setDisplayName(name)
                                .setDeliveryEstimate(SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.builder()
                                        .setMinimum(SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Minimum.builder()
                                                .setUnit(SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Minimum.Unit.BUSINESS_DAY)
                                                .setValue(minDays)
                                                .build())
                                        .setMaximum(SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Maximum.builder()
                                                .setUnit(SessionCreateParams.ShippingOption.ShippingRateData.DeliveryEstimate.Maximum.Unit.BUSINESS_DAY)
                                                .setValue(maxDays)
                                                .build())
                                        .build())
                                .build())
                .build();
    }
}
