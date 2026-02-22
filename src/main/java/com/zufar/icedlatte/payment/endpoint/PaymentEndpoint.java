package com.zufar.icedlatte.payment.endpoint;

import com.zufar.icedlatte.openapi.dto.*;
import com.zufar.icedlatte.payment.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
public class PaymentEndpoint implements com.zufar.icedlatte.openapi.payment.api.PaymentApi {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentProcessor paymentProcessor;
    private final WebhookEventProcessor webhookEventProcessor;
    private final RedirectEventProcessor redirectEventProcessor;

    @Override
    @PostMapping
    public ResponseEntity<SessionWithClientSecretDto> processPayment() {
        log.info("payment.processing");
        var response = paymentProcessor.processPayment(null);
        log.info("payment.session.created: sessionId={}", response.getSessionId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> processStripeWebhook(@RequestHeader("Stripe-Signature") String stripeSignature,
                                                     @RequestBody String payload) {
        log.info("payment.webhook.receiving");
        webhookEventProcessor.processPaymentEvent(payload, stripeSignature);
        log.info("payment.webhook.received");
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/order")
    public ResponseEntity<PaymentConfirmationEmail> processRedirectEvent(@RequestParam String sessionId) {
        log.info("payment.redirect.processing");
        var confirmation = redirectEventProcessor.processPaymentEvent(sessionId);
        log.info("payment.redirect.processed");
        return ResponseEntity.ok(confirmation);
    }
}