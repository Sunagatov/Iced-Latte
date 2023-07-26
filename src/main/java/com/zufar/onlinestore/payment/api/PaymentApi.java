package com.zufar.onlinestore.payment.api;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;

public interface PaymentApi {

    PaymentDetailsWithTokenDto createPayment(CreatePaymentDto createPaymentDto) throws StripeException;

    String createPaymentMethod(CreatePaymentMethodDto createPaymentDto) throws StripeException;

    PaymentDetailsDto getPaymentDetails(Long paymentId) throws PaymentNotFoundException;

    void paymentEventProcess(String paymentIntentPayload, String stripeSignatureHeader) throws SignatureVerificationException;

}