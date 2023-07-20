package com.zufar.onlinestore.payment;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.controller.PaymentController;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentWithTokenDetailsDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Validated
@RequestMapping(PaymentController.PAYMENT_URL)
public interface PaymentApi {

    @PostMapping
    ResponseEntity<PaymentWithTokenDetailsDto> paymentProcess(@RequestBody @Valid CreatePaymentDto paymentDto) throws StripeException;

    @GetMapping("/{paymentId}")
    ResponseEntity<PaymentDetailsDto> getPaymentDetails(@PathVariable Long paymentId);

}