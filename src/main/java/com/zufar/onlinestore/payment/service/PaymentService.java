package com.zufar.onlinestore.payment.service;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentWithTokenDetailsDto;
import java.math.BigDecimal;

public interface PaymentService {

    PaymentWithTokenDetailsDto createPayment(String paymentMethodId, BigDecimal totalPrice, String currency) throws StripeException;

    PaymentDetailsDto getPayment(Long paymentId);
}

