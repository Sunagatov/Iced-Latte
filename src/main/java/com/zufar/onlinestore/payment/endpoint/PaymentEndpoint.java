package com.zufar.onlinestore.payment.endpoint;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Objects;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
public class PaymentEndpoint {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentApi paymentApi;

    @PostMapping
    public ResponseEntity<PaymentDetailsWithTokenDto> createPayment(@RequestBody @Valid final CreatePaymentDto createPaymentDto) throws StripeException {
        PaymentDetailsWithTokenDto createdPayment = paymentApi.createPayment(createPaymentDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdPayment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailsDto> getPaymentDetails(@PathVariable @Valid @NotNull Long paymentId) {
        PaymentDetailsDto retrievedPayment = paymentApi.getPaymentDetails(paymentId);
        if (Objects.isNull(retrievedPayment)) {
            log.info("Get payment details: not found payment details by id: {}.", paymentId);
            return ResponseEntity.notFound()
                    .build();
        }
        log.info("Get payment details: payment details: {} successfully retrieved.", retrievedPayment);

        return ResponseEntity.ok()
                .body(retrievedPayment);
    }

    /**
     * This endpoint is used only until we have an implementation of this logic on the frontend side.
     * It will come in handy for testing the API.
     */
    @PostMapping("/method")
    public ResponseEntity<String> getPaymentMethod(@RequestBody @Valid final CreatePaymentMethodDto createPaymentMethodDto) throws StripeException {
        String paymentMethodId = paymentApi.createPaymentMethod(createPaymentMethodDto);
        return ResponseEntity.ok()
                .body(paymentMethodId);
    }

    @PostMapping("/event")
    public ResponseEntity<Void> paymentEventsProcess(
            @RequestBody @Valid @NotEmpty @NotNull String paymentIntentPayload,
            @RequestHeader("Stripe-Signature") @Valid @NotEmpty @NotNull String stripeSignatureHeader) throws SignatureVerificationException {
        if (Objects.isNull(paymentIntentPayload) || Objects.isNull(stripeSignatureHeader)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        paymentApi.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);

        return ResponseEntity.ok()
                .build();
    }
}