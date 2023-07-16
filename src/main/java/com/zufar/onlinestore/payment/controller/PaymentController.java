package com.zufar.onlinestore.payment.controller;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.PaymentApi;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentResponseDto;
import com.zufar.onlinestore.payment.service.PaymentService;
import com.zufar.onlinestore.review.controller.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Objects;


@Slf4j
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    public ResponseEntity<ApiResponse<PaymentDetailsDto>> paymentProcess(CreatePaymentDto request) throws StripeException {
        log.debug("Received request to create Payment - {}.", request);
        PaymentDetailsDto processedPayment = paymentService.createPayment(request.paymentMethodId(), request.totalPrice(), request.currency());
        log.debug("Processed payment  response - {}.", processedPayment);

        ApiResponse<PaymentDetailsDto> apiResponse = ApiResponse.<PaymentDetailsDto>builder()
                .data(processedPayment)
                .message(PAYMENT_CREATE.formatted(processedPayment.paymentResponseDto().paymentId()))
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.CREATED.value())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);

    }


    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentDetails(String paymentId) {
        log.debug("Received request to get Payment by Id {}", paymentId);
        PaymentResponseDto retrievedPayment = paymentService.getPayment(paymentId);
        if (Objects.isNull(retrievedPayment)) {
            log.debug("Payment by Id {} are absent.", paymentId);
            return ResponseEntity.notFound().build();
        }
        log.debug("Payment retrieved {}.", retrievedPayment);

        ApiResponse<PaymentResponseDto> apiResponse = ApiResponse.<PaymentResponseDto>builder()
                .data(retrievedPayment)
                .message(PAYMENT_RETRIEVE.formatted(retrievedPayment.paymentId()))
                .timeStamp(LocalDateTime.now())
                .status(HttpStatus.OK.value())
                .build();

        return ResponseEntity.ok().body(apiResponse);
    }
}
