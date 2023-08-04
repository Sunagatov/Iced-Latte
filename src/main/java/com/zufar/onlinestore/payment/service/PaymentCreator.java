package com.zufar.onlinestore.payment.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.payment.converter.PaymentIntentConverter;
import com.zufar.onlinestore.payment.dto.CreatePaymentDto;
import com.zufar.onlinestore.payment.dto.PaymentDetailsWithTokenDto;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.converter.PaymentConverter;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for filling payment entity based on payment intent stripe object,
 * saving payment entity in database and for transferring payment token, which using on the front-end
 * and Stripe API sides to process payment.
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentCreator {

    private final PaymentRepository paymentRepository;
    private final PaymentIntentCreator paymentIntentCreator;
    private final PaymentIntentConverter paymentIntentConverter;
    private final PaymentConverter paymentConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public PaymentDetailsWithTokenDto createPayment(final CreatePaymentDto createPaymentDto) throws StripeException {
        PaymentIntent paymentIntent = paymentIntentCreator.createPaymentIntent(createPaymentDto);
        log.info("Create payment: payment intent: {} successfully created.", paymentIntent);
        String paymentToken = paymentIntent.getClientSecret();
        Payment payment = paymentIntentConverter.toPayment(paymentIntent);
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Create payment: payment {} successfully saved.", savedPayment);

        return PaymentDetailsWithTokenDto.builder()
                .paymentToken(paymentToken)
                .paymentDetailsDto((paymentConverter.toDto(savedPayment)))
                .build();
    }

}
