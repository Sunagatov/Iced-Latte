package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.model.PaymentIntent;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.payment.converter.StripePaymentIntentConverter;
import com.zufar.onlinestore.payment.entity.Payment;
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
    private final StripePaymentIntentConverter stripePaymentIntentConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Payment createPayment(final PaymentIntent paymentIntent, final ShoppingSessionDto shoppingSession) {
        log.info("Create payment intent: in progress: creation payment with stripe paymentIntentId = {}", paymentIntent.getId());
        Payment payment = stripePaymentIntentConverter.toEntity(paymentIntent, shoppingSession);
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Create payment intent: successful: payment was created with paymentId = {}", savedPayment.getPaymentId());
        return savedPayment;
    }

}
