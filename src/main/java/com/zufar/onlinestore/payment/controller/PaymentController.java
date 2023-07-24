package com.zufar.onlinestore.payment.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.PaymentApi;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.dto.PriceDetailsDto;
import com.zufar.onlinestore.payment.service.PaymentEventService;
import com.zufar.onlinestore.payment.service.PaymentService;
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


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentController.PAYMENT_URL)
public class PaymentController implements PaymentApi {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentService paymentService;
    private final PaymentEventService paymentEventService;

    @PostMapping
    public ResponseEntity<PaymentDetailsWithTokenDto> paymentProcess(@RequestBody CreatePaymentDto paymentRequest) throws StripeException {
        if (paymentRequest == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("payment process: receive request to create payment: paymentRequest: {}.", paymentRequest);
        PriceDetailsDto priceDetails = paymentRequest.priceDetails();
        PaymentDetailsWithTokenDto processedPayment =
                paymentService.createPayment(paymentRequest.paymentMethodId(), priceDetails.totalPrice(), priceDetails.currency());
        log.info("payment process: payment successfully processed: processedPayment: {}.", processedPayment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(processedPayment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDetailsDto> getPaymentDetails(@PathVariable Long paymentId) {
        log.info("get payment details: receive payment id: paymentId: {}.", paymentId);
        PaymentDetailsDto retrievedPayment = paymentService.getPayment(paymentId);
        if (retrievedPayment == null) {
            log.info("get payment details: not found payment details by id: paymentId: {}.", paymentId);
            return ResponseEntity.notFound()
                    .build();
        }
        log.info("get payment details: payment successfully retrieved: retrievedPayment: {}.", retrievedPayment);

        return ResponseEntity.ok()
                .body(retrievedPayment);
    }

    @PostMapping("/events")
    public ResponseEntity<Void> paymentEventsProcess(@RequestBody String payload, @RequestHeader("Stripe-Signature") String header) throws SignatureVerificationException {
        if (header == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        paymentEventService.processPaymentEvents(payload, header);

        return ResponseEntity.ok().build();
    }
}