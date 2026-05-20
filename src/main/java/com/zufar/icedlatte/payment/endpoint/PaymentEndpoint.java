package com.zufar.icedlatte.payment.endpoint;

import com.zufar.icedlatte.common.http.ApiPaths;
import com.zufar.icedlatte.openapi.dto.CheckoutResponseDto;
import com.zufar.icedlatte.openapi.dto.CheckoutStatusDto;
import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.payment.api.checkout.CheckoutPaymentService;
import com.zufar.icedlatte.payment.api.PaymentStatusService;
import com.zufar.icedlatte.payment.api.webhook.StripeWebhookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Stripe Hosted Checkout endpoints (test mode only — no real money).
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(ApiPaths.PAYMENT)
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@SuppressWarnings("unused") // Spring MVC invokes endpoint methods via reflection.
public class PaymentEndpoint implements com.zufar.icedlatte.openapi.payment.api.PaymentApi {

    private final CheckoutPaymentService checkoutPaymentService;
    private final PaymentStatusService paymentStatusService;
    private final StripeWebhookService stripeWebhookService;

    @Override
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDto> createCheckout(
            @NotNull @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateCheckoutRequestDto request) {
        CheckoutResponseDto response = checkoutPaymentService.checkout(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/checkout/{orderId}/status")
    public ResponseEntity<CheckoutStatusDto> getCheckoutStatus(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentStatusService.getStatus(orderId));
    }

    @Override
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> processStripeWebhook(
            @NotNull @RequestHeader("Stripe-Signature") String stripeSignature,
            @Valid @RequestBody String body) {
        stripeWebhookService.processWebhook(body, stripeSignature);
        return ResponseEntity.ok().build();
    }
}
