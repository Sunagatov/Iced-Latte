package com.zufar.onlinestore.payment.service.impl;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.dto.PaymentResponseDto;
import com.zufar.onlinestore.payment.model.Payment;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.mapper.PaymentConverter;
import com.zufar.onlinestore.payment.model.PaymentDetails;
import com.zufar.onlinestore.payment.processor.PaymentProcessor;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import com.zufar.onlinestore.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProcessor paymentProcessor;
    private final PaymentRepository paymentRepository;
    private final PaymentConverter mapper;

    public PaymentDetailsDto createPayment(String paymentMethodId, BigDecimal totalPrice, String currency) throws StripeException {
        log.debug("Calling a process method from {}.", PaymentProcessor.class);
        PaymentDetails processedPaymentDetails = paymentProcessor.process(paymentMethodId, totalPrice, currency);
        log.debug("Processed payment details = {}.", processedPaymentDetails);
        String paymentToken = processedPaymentDetails.paymentToken();
        Payment savedPayment = paymentRepository.save(processedPaymentDetails.payment());

        return PaymentDetailsDto.builder()
                .paymentToken(paymentToken)
                .paymentResponseDto(mapper.toDto(savedPayment))
                .build();
    }

    public PaymentResponseDto getPayment(String paymentId) {
        Optional<Payment> retrievedPayment = paymentRepository.findById(paymentId);
        return retrievedPayment.map(mapper::toDto)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
