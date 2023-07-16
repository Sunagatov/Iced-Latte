package com.zufar.onlinestore.payment.service;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentResponseDto;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentDetailsDto createPayment(String paymentMethodId, BigDecimal totalPrice, String currency) throws StripeException;

    PaymentResponseDto getPayment(String paymentId);
}
