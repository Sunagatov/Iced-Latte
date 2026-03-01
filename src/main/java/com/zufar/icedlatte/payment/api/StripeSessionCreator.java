package com.zufar.icedlatte.payment.api;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.cart.api.ShoppingCartProvider;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.SessionWithClientSecretDto;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.payment.converter.StripeSessionLineItemListConverter;
import com.zufar.icedlatte.payment.exception.StripeSessionCreationException;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class StripeSessionCreator {

    private static final String RETURN_URI = "/orders?sessionId={CHECKOUT_SESSION_ID}";

    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final StripeSessionLineItemListConverter lineItemConverter;
    private final ShoppingCartProvider shoppingCartProvider;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    private void initStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    public SessionWithClientSecretDto createSession(final HttpServletRequest request) {
        log.info("payment.session.initiating");
        UserDto user = securityPrincipalProvider.get();
        UUID userId = user.getId();
        ShoppingCartDto cart = shoppingCartProvider.getByUserId(userId);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED)
                .setCustomerEmail(user.getEmail())
                .setReturnUrl(buildReturnUrl(request))
                .addAllLineItem(lineItemConverter.toLineItems(cart.getItems()))
                .addAllShippingOption(shippingOptions())
                .putMetadata("userId", userId.toString())
                .build();

        try {
            Session session = Session.create(params);
            return new SessionWithClientSecretDto()
                    .sessionId(session.getId())
                    .clientSecret(session.getClientSecret());
        } catch (StripeException e) {
            throw new StripeSessionCreationException(e.getMessage(), e);
        }
    }

    private String buildReturnUrl(HttpServletRequest request) {
        return UriComponentsBuilder.newInstance()
                .scheme(request.getScheme())
                .host(request.getHeader(HttpHeaders.HOST))
                .path(RETURN_URI)
                .build()
                .toUriString();
    }

    private List<SessionCreateParams.ShippingOption> shippingOptions() {
        return List.of(
                buildShippingOption("Free shipping", 0L, 5L, 7L),
                buildShippingOption("Next day air", 1500L, 1L, 1L)
        );
    }

    private SessionCreateParams.ShippingOption buildShippingOption(String name, long amountCents, long minDays, long maxDays) {
        return SessionCreateParams.ShippingOption.builder()
                .setShippingRateData(
                        SessionCreateParams.ShippingOption.ShippingRateData.builder()
                                .setType(SessionCreateParams.ShippingOption.ShippingRateData.Type.FIXED_AMOUNT)
                                .setFixedAmount(SessionCreateParams.ShippingOption.ShippingRateData.FixedAmount.builder()
                                        .setAmount(amountCents)
                                        .setCurrency("usd")
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
