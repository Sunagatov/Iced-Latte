package com.zufar.onlinestore.payment.endpoint;

import com.zufar.onlinestore.common.response.ApiResponse;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
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
import java.util.List;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping(PaymentEndpoint.PAYMENT_URL)
public class PaymentEndpoint {

    public static final String PAYMENT_URL = "/api/v1/payment";

    private final PaymentApi paymentApi;

    @PostMapping
    public ResponseEntity<ApiResponse<ProcessedPaymentWithClientSecretDto>> processPayment(@RequestParam @NotEmpty final String cardDetailsTokenId) {
        ProcessedPaymentWithClientSecretDto processedPayment = paymentApi.processPayment(cardDetailsTokenId);

        ApiResponse<ProcessedPaymentWithClientSecretDto> apiResponse = ApiResponse.<ProcessedPaymentWithClientSecretDto>builder()
                .data(processedPayment)
                .messages(List.of("Payment successfully processed"))
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.CREATED.value())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<ProcessedPaymentDetailsDto>> getPaymentDetails(@PathVariable @NotNull final Long paymentId) {
        ProcessedPaymentDetailsDto retrievedPayment = paymentApi.getPaymentDetails(paymentId);

        ApiResponse<ProcessedPaymentDetailsDto> apiResponse = ApiResponse.<ProcessedPaymentDetailsDto>builder()
                .data(retrievedPayment)
                .messages(List.of("Payment successfully retrieved"))
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.OK.value())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiResponse);
    }

    @PostMapping("/event")
    public ResponseEntity<ApiResponse<Void>> paymentEventProcess(@RequestBody @NotEmpty final String paymentIntentPayload,
                                                     @RequestHeader("Stripe-Signature") @NotEmpty final String stripeSignatureHeader) {

        paymentApi.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);

        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .messages(List.of("Payment event successfully processed"))
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.OK.value())
                .build();

        return ResponseEntity.status(HttpStatus.OK)
                .body(apiResponse);
    }

    @PostMapping("/card")
    public ResponseEntity<ApiResponse<String>> processCardDetailsToken(@RequestBody @Valid final CreateCardDetailsTokenDto createCardDetailsTokenDto) {

        String cardDetailsTokenId = paymentApi.processCardDetailsToken(createCardDetailsTokenDto);
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .data(cardDetailsTokenId)
                .messages(List.of("Card details token successfully processed"))
                .timestamp(LocalDateTime.now())
                .httpStatusCode(HttpStatus.CREATED.value())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(apiResponse);
    }
}