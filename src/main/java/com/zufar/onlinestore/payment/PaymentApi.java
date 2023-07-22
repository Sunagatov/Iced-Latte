package com.zufar.onlinestore.payment;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import org.springframework.http.ResponseEntity;

public interface PaymentApi {

    ResponseEntity<PaymentDetailsWithTokenDto> paymentProcess(CreatePaymentDto paymentDto);

    ResponseEntity<PaymentDetailsDto> getPaymentDetails(Long paymentId) throws PaymentNotFoundException;

}