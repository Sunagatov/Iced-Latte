package com.zufar.onlinestore.payment.service;

import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.converter.PaymentConverter;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentRetriever {

    private final PaymentRepository paymentRepository;
    private final PaymentConverter paymentConverter;

    public PaymentDetailsDto getPaymentDetails(Long paymentId) {
        Objects.requireNonNull(paymentId);
        log.info("Get payment details: start payment details retrieve by payment id: {}.", paymentId);

        return paymentRepository.findById(paymentId)
                .map(paymentConverter::toDto)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
