package com.zufar.onlinestore.payment;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.controller.PaymentController;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentResponseDto;
import com.zufar.onlinestore.review.controller.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@Validated
@RequestMapping(PaymentController.PAYMENT_URL)
public interface PaymentApi {

    String PAYMENT_URL = "/api/v1/payment";
    String PAYMENT_CREATE = "Payment with id - %s created";
    String PAYMENT_RETRIEVE = "Payment with id - %s retrieved";

    @PostMapping("/process")
    ResponseEntity<ApiResponse<PaymentDetailsDto>> paymentProcess(@RequestBody @Valid @NotNull(message = "Request body is mandatory") CreatePaymentDto paymentDto) throws StripeException;

    @GetMapping("/{paymentId}")
    ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentDetails(@PathVariable String paymentId);

}
