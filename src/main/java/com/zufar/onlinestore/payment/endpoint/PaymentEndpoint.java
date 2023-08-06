package com.zufar.onlinestore.payment.endpoint;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.api.dto.ProcessPaymentDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import jakarta.validation.Valid;
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

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
public class PaymentEndpoint {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentApi paymentApi;

    @PostMapping
    public ResponseEntity<ProcessedPaymentWithClientSecretDto> createPayment(@RequestBody @Valid final ProcessPaymentDto processPaymentDto) throws StripeException {
        ProcessedPaymentWithClientSecretDto createdPayment = paymentApi.processPayment(processPaymentDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdPayment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ProcessedPaymentDetailsDto> getPaymentDetails(@PathVariable @NotNull final Long paymentId) throws PaymentNotFoundException {
        ProcessedPaymentDetailsDto retrievedPayment = paymentApi.getPaymentDetails(paymentId);
        log.info("Get payment details: payment details: {} successfully retrieved.", retrievedPayment);
        return ResponseEntity.ok()
                .body(retrievedPayment);
    }

    @PostMapping("/event")
    public ResponseEntity<Void> paymentEventsProcess(@RequestBody @NotEmpty final String paymentIntentPayload,
                                                     @RequestHeader("Stripe-Signature") @NotEmpty final String stripeSignatureHeader) throws SignatureVerificationException {
        paymentApi.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
        return ResponseEntity.ok()
                .build();
    }
}