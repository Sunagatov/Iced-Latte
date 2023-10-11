package com.zufar.onlinestore.payment.endpoint;

import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenRequest;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
public class PaymentEndpoint implements com.zufar.onlinestore.openapi.payment.api.PaymentApi {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentApi paymentApi;

    @Override
    @PostMapping
    public ResponseEntity<ProcessedPaymentWithClientSecretDto> processPayment(@RequestParam final String cardDetailsTokenId) {
        ProcessedPaymentWithClientSecretDto processedPayment = paymentApi.processPayment(cardDetailsTokenId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(processedPayment);
    }

    @Override
    @GetMapping("/{paymentId}")
    public ResponseEntity<ProcessedPaymentDetailsDto> getPaymentDetails(@PathVariable final Long paymentId) {
        ProcessedPaymentDetailsDto retrievedPayment = paymentApi.getPaymentDetails(paymentId);

        return ResponseEntity.status(HttpStatus.OK)
                .body(retrievedPayment);
    }

    @Override
    @PostMapping("/event")
    public ResponseEntity<Void> paymentEventProcess(@RequestBody final String paymentIntentPayload, @RequestHeader("Stripe-Signature") final String stripeSignatureHeader) {
        paymentApi.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @PostMapping("/card")
    public ResponseEntity<String> processCardDetailsToken(@RequestBody final CreateCardDetailsTokenRequest createCardDetailsTokenRequest) {
        String cardDetailsTokenId = paymentApi.processCardDetailsToken(createCardDetailsTokenRequest);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardDetailsTokenId);
    }
}