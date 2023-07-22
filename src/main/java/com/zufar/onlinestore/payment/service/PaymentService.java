package com.zufar.onlinestore.payment.service;

import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentDetailsWithTokenDto createPayment(String paymentMethodId, BigDecimal totalPrice, String currency);

    PaymentDetailsDto getPayment(Long paymentId) throws PaymentNotFoundException;

}

