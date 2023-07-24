package com.zufar.onlinestore.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.enums.PaymentConstants;
import com.zufar.onlinestore.payment.mapper.PaymentConverter;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentCreator {

    private final PaymentIntentCreator paymentIntentCreator;
    private final PaymentRepository paymentRepository;
    private final PaymentConverter paymentConverter;

    public PaymentDetailsWithTokenDto createPayment(String paymentMethodId, BigDecimal totalPrice, String currency) throws StripeException {
        PaymentIntent paymentIntent = paymentIntentCreator.createPaymentIntent(paymentMethodId, totalPrice, currency);
        log.info("create payment: payment intent successfully created: paymentIntentId: {}.", paymentIntent.getId());
        String paymentToken = paymentIntent.getClientSecret();
        Payment payment = fillPaymentDetails(paymentIntent);
        Payment savedPayment = paymentRepository.save(payment);
        log.info("create payment: payment successfully saved: savedPayment: {}.", savedPayment);

        return PaymentDetailsWithTokenDto.builder()
                .paymentToken(paymentToken)
                .paymentDetailsDto((paymentConverter.toDto(savedPayment)))
                .build();
    }


    private Payment fillPaymentDetails(PaymentIntent paymentIntent) {
        Integer delimiter = PaymentConstants.PAYMENT_DELIMITER.getValue();
        return Payment.builder()
                .itemsTotalPrice(BigDecimal.valueOf(paymentIntent.getAmount() / delimiter))
                .paymentIntentId(paymentIntent.getId())
                .currency(paymentIntent.getCurrency())
                .build();
    }
}
