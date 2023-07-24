package com.zufar.onlinestore.payment.service;

import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.mapper.PaymentConverter;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentGetter {

    private final PaymentRepository paymentRepository;
    private final PaymentConverter paymentConverter;

    public PaymentDetailsDto getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(paymentConverter::toDto)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
