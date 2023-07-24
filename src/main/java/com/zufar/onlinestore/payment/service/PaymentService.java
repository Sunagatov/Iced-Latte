package com.zufar.onlinestore.payment.service;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentDetailsWithTokenDto createPayment(String paymentMethodId,
                                             BigDecimal totalPrice,
                                             String currency) throws StripeException;

    PaymentDetailsDto getPayment(Long paymentId) throws PaymentNotFoundException;
}

