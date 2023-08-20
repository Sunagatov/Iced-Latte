package com.zufar.onlinestore.payment.endpoint;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.exception.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import java.time.LocalDateTime;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
public class PaymentEndpoint {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentApi paymentApi;

    @PostMapping
    public ResponseEntity<ApiResponse<ProcessedPaymentWithClientSecretDto>> processPayment(@RequestParam @NotEmpty final String cardDetailsTokenId) throws PaymentMethodProcessingException, StripeCustomerProcessingException, PaymentIntentProcessingException {
        ProcessedPaymentWithClientSecretDto processedPayment = paymentApi.processPayment(cardDetailsTokenId);

        ApiResponse<ProcessedPaymentWithClientSecretDto> apiResponse = ApiResponse.<ProcessedPaymentWithClientSecretDto>builder()
                .data(processedPayment)
                .message("Payment successfully processed")
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.CREATED.value())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<ProcessedPaymentDetailsDto>> getPaymentDetails(@PathVariable @NotNull final Long paymentId) throws PaymentNotFoundException {
        ProcessedPaymentDetailsDto retrievedPayment = paymentApi.getPaymentDetails(paymentId);

        ApiResponse<ProcessedPaymentDetailsDto> apiResponse = ApiResponse.<ProcessedPaymentDetailsDto>builder()
                .data(retrievedPayment)
                .message("Payment successfully retrieved")
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.OK.value())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiResponse);
    }

    @PostMapping("/event")
    public ResponseEntity<ApiResponse<Void>> paymentEventProcess(@RequestBody @NotEmpty final String paymentIntentPayload,
                                                     @RequestHeader("Stripe-Signature") @NotEmpty final String stripeSignatureHeader) throws PaymentEventProcessingException, PaymentEventParsingException {

        paymentApi.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .message("Payment event successfully processed")
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.OK.value())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiResponse);
    }

    @Deprecated
    @PostMapping("/card")
    public ResponseEntity<ApiResponse<String>> processCardDetailsToken(@RequestBody @Valid final CreateCardDetailsTokenDto createCardDetailsTokenDto) throws StripeException {

        String cardDetailsTokenId = paymentApi.processCardDetailsToken(createCardDetailsTokenDto);
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .data(cardDetailsTokenId)
                .message("Card details token successfully processed")
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.CREATED.value())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse);
    }
}