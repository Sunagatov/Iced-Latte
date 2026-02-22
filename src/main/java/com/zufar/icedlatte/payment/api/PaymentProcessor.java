package com.zufar.icedlatte.payment.api;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.openapi.dto.SessionWithClientSecretDto;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentProcessor {

    private final StripeSessionCreator stripeSessionCreator;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    private void initStripe() {
        Stripe.apiKey = stripeSecretKey;
    }

    public SessionWithClientSecretDto processPayment(final HttpServletRequest request) {
        log.info("payment.session.initiating");
        Session stripeSession = stripeSessionCreator.createSession(request);

        SessionWithClientSecretDto sessionDto = new SessionWithClientSecretDto();
        sessionDto.setSessionId(stripeSession.getId());
        sessionDto.setClientSecret(stripeSession.getClientSecret());

        return sessionDto;
    }
}
