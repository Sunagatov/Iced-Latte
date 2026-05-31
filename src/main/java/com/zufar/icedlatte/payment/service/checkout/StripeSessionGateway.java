package com.zufar.icedlatte.payment.service.checkout;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import com.zufar.icedlatte.payment.config.StripeProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(StripeProperties.class)
public class StripeSessionGateway {

    private final StripeProperties stripeProperties;

    public Session create(SessionCreateParams params, String idempotencyKey) throws StripeException {
        return Session.create(params, requestOptions(idempotencyKey));
    }

    public Session retrieve(String sessionId) throws StripeException {
        return Session.retrieve(sessionId, requestOptions(null));
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder().setApiKey(stripeProperties.secretKey());
        if (idempotencyKey != null) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }
}
