package com.zufar.onlinestore.payment.api.impl.intent;

import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.payment.converter.PaymentConverter;
import com.zufar.onlinestore.openapi.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.UUID;

/**
 * This class is responsible for retrieving relevant payment details from database
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentRetriever {

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentConverter paymentConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ProcessedPaymentDetailsDto getPaymentDetails(final Long paymentId) {
        Objects.requireNonNull(paymentId);
        log.info("Get payment details: starting: payment details retrieve by payment id = {}.", paymentId);

        return paymentRepository.findById(paymentId)
                .map(payment -> {
                    UUID shoppingSessionId = payment.getShoppingSessionId();
                    return shoppingSessionRepository.findById(shoppingSessionId)
                            .map(session -> paymentConverter.toDto(payment, session.getItems()))
                            .orElseThrow(() -> new ShoppingSessionNotFoundException(shoppingSessionId));
                })
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
