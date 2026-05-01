package com.zufar.icedlatte.payment.endpoint;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.PaymentConfirmationEmail;
import com.zufar.icedlatte.openapi.dto.SessionWithClientSecretDto;
import com.zufar.icedlatte.payment.api.StripeSessionCreator;
import com.zufar.icedlatte.payment.api.StripeWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class PaymentEndpoint implements com.zufar.icedlatte.openapi.payment.api.PaymentApi {

    public static final String PAYMENT_URL = ApiPaths.PAYMENT;

    private final StripeSessionCreator stripeSessionCreator;
    private final StripeWebhookService stripeWebhookService;
    private final HttpServletRequest httpRequest;

    @Override
    @PostMapping
    public ResponseEntity<SessionWithClientSecretDto> processPayment() {
        SessionWithClientSecretDto response = stripeSessionCreator.createSession(httpRequest);
        log.info("payment.session.created: sessionId={}", maskSessionId(response.getSessionId()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> processStripeWebhook(@RequestHeader("Stripe-Signature") String stripeSignature,
                                                     @RequestBody String payload) {
        log.debug("payment.webhook.receiving");
        stripeWebhookService.processWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/order")
    public ResponseEntity<PaymentConfirmationEmail> processRedirectEvent(@RequestParam String sessionId) {
        return ResponseEntity.ok(stripeWebhookService.processRedirect(sessionId));
    }

    private static String maskSessionId(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return "unknown";
        }
        return StringUtils.left(StringUtils.overlay(sessionId, "****", 6, sessionId.length()), 10);
    }
}
