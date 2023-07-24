package com.zufar.onlinestore.payment.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.zufar.onlinestore.payment.PaymentApi;
import com.zufar.onlinestore.payment.dto.*;
import com.zufar.onlinestore.payment.service.PaymentCreator;
import com.zufar.onlinestore.payment.service.PaymentEventProcessor;
import com.zufar.onlinestore.payment.service.PaymentGetter;
import com.zufar.onlinestore.payment.service.PaymentMethodGetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentController.PAYMENT_URL)
public class PaymentController implements PaymentApi {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentCreator paymentCreator;
    private final PaymentGetter paymentGetter;
    private final PaymentMethodGetter paymentMethodGetter;
    private final PaymentEventProcessor paymentEventProcessor;

    @PostMapping
    public ResponseEntity<PaymentDetailsWithTokenDto> paymentProcess(@RequestBody CreatePaymentDto paymentRequest) throws StripeException {
        if (Objects.isNull(paymentRequest)) {
            return ResponseEntity.badRequest().build();
        }
        log.info("payment process: receive request to create payment: paymentRequest: {}.", paymentRequest);
        PriceDetailsDto priceDetails = paymentRequest.priceDetails();
        PaymentDetailsWithTokenDto processedPayment =
                paymentCreator.createPayment(paymentRequest.paymentMethodId(), priceDetails.totalPrice(), priceDetails.currency());
        log.info("payment process: payment successfully processed: processedPayment: {}.", processedPayment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(processedPayment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailsDto> getPaymentDetails(@PathVariable Long paymentId) {
        log.info("get payment details: receive payment id: paymentId: {}.", paymentId);
        PaymentDetailsDto retrievedPayment = paymentGetter.getPayment(paymentId);
        if (Objects.isNull(retrievedPayment)) {
            log.info("get payment details: not found payment details by id: paymentId: {}.", paymentId);
            return ResponseEntity.notFound()
                    .build();
        }
        log.info("get payment details: payment successfully retrieved: retrievedPayment: {}.", retrievedPayment);

        return ResponseEntity.ok()
                .body(retrievedPayment);
    }

    /**
     * This endpoint is used only until we have an implementation of this logic on the frontend side.
     * It will come in handy for testing the API.
     */
    @PostMapping("/method")
    public ResponseEntity<String> getPaymentMethodId(@RequestBody CreatePaymentMethodDto paymentRequest) throws StripeException {
        return ResponseEntity.ok().body(paymentMethodGetter.getPaymentMethodId(paymentRequest));
    }

    @PostMapping("/event")
    public ResponseEntity<Void> paymentEventsProcess(@RequestBody String paymentIntentPayload, @RequestHeader("Stripe-Signature") String stripeSignatureHeader) throws SignatureVerificationException {
        if (Objects.isNull(paymentIntentPayload) || Objects.isNull(stripeSignatureHeader)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        paymentEventProcessor.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);

        return ResponseEntity.ok().build();
    }
}