package com.zufar.onlinestore.payment.api.impl.intent;

import com.zufar.onlinestore.payment.converter.PaymentConverter;
import com.zufar.onlinestore.payment.dto.PaymentDetailsDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * This class is responsible for retrieving relevant payment details from database
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentRetriever {

    private final PaymentRepository paymentRepository;
    private final PaymentConverter paymentConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public PaymentDetailsDto getPaymentDetails(Long paymentId) throws PaymentNotFoundException {
        Objects.requireNonNull(paymentId);
        log.info("Get payment details: start payment details retrieve by payment id: {}.", paymentId);

        return paymentRepository.findById(paymentId)
                .map(paymentConverter::toDto)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
