package com.zufar.onlinestore.payment.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.mapper.PaymentConverter;
import com.zufar.onlinestore.payment.processor.PaymentProcessor;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import com.zufar.onlinestore.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProcessor paymentProcessor;
    private final PaymentRepository paymentRepository;
    private final PaymentConverter paymentConverter;

    public PaymentDetailsWithTokenDto createPayment(String paymentMethodId, BigDecimal totalPrice, String currency) throws StripeException {
        Pair<String, Payment> paymentWithTokenDetails = paymentProcessor.processPayment(paymentMethodId, totalPrice, currency);
        log.info("create payment: payment successfully processed: paymentWithTokenDetails: {}.", paymentWithTokenDetails);
        Payment savedPayment = paymentRepository.save(paymentWithTokenDetails.getValue());
        log.info("create payment: payment successfully saved: savedPayment: {}.", savedPayment);
        return PaymentDetailsWithTokenDto.builder()
                .paymentToken(paymentWithTokenDetails.getKey())
                .paymentDetailsDto((paymentConverter.toDto(savedPayment)))
                .build();
    }

    public PaymentDetailsDto getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(paymentConverter::toDto)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
