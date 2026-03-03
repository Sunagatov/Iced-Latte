package com.zufar.icedlatte.payment.endpoint;

import com.zufar.icedlatte.openapi.dto.PaymentConfirmationEmail;
import com.zufar.icedlatte.openapi.dto.SessionWithClientSecretDto;
import com.zufar.icedlatte.payment.api.StripeSessionCreator;
import com.zufar.icedlatte.payment.api.StripeWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class PaymentEndpoint implements com.zufar.icedlatte.openapi.payment.api.PaymentApi {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final StripeSessionCreator stripeSessionCreator;
    private final StripeWebhookService stripeWebhookService;
    private final HttpServletRequest httpRequest;

    @Override
    @PostMapping
    public ResponseEntity<SessionWithClientSecretDto> processPayment() {
        SessionWithClientSecretDto response = stripeSessionCreator.createSession(httpRequest);
        log.info("payment.session.created: sessionId={}", response.getSessionId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> processStripeWebhook(@RequestHeader("Stripe-Signature") String stripeSignature,
                                                     @RequestBody String payload) {
        log.info("payment.webhook.receiving");
        stripeWebhookService.processWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/order")
    public ResponseEntity<PaymentConfirmationEmail> processRedirectEvent(@RequestParam String sessionId) {
        return ResponseEntity.ok(stripeWebhookService.processRedirect(sessionId));
    }
}
