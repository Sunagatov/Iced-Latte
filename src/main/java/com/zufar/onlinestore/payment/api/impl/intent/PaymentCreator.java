package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.payment.converter.StripePaymentIntentConverter;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.enums.PaymentStatus;
import com.zufar.onlinestore.payment.exception.ShoppingSessionAlreadyPaidException;
import com.zufar.onlinestore.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * This class is responsible for filling payment entity based on payment intent stripe object,
 * saving payment entity in database and for transferring payment token, which using on the front-end
 * and Stripe API sides to process payment.
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentCreator {

    private final CartApi cartApi;
    private final PaymentRepository paymentRepository;
    private final StripePaymentIntentConverter stripePaymentIntentConverter;
    private final StripePaymentIntentCreator stripePaymentIntentCreator;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Pair<String, Payment> createPayment(final Pair<UUID, PaymentMethod> pair) {
        log.info("Create payment: starting: start payment creation");
        UUID userId = pair.getLeft();
        PaymentMethod paymentMethod = pair.getRight();

        ShoppingSessionDto shoppingSession = cartApi.getShoppingSessionByUserId(userId);
        PaymentIntent stripePaymentIntent = stripePaymentIntentCreator.createStripePaymentIntent(paymentMethod, shoppingSession);
        Payment paymentToSave = fillPaymentDetails(shoppingSession, stripePaymentIntent);
        try {
            Payment savedPayment = paymentRepository.save(paymentToSave);
            log.info("Create payment: finishing: payment was created");

            return Pair.of(stripePaymentIntent.getClientSecret(), savedPayment);

        } catch (DataIntegrityViolationException e) {
           throw new ShoppingSessionAlreadyPaidException(shoppingSession.getId());
        }
    }

    private Payment fillPaymentDetails(ShoppingSessionDto shoppingSession, PaymentIntent stripePaymentIntent) {
        log.info("Fill payment details: starting: start payment object filling");
        Payment payment = stripePaymentIntentConverter.toEntity(stripePaymentIntent, shoppingSession);
        payment.setStatus(PaymentStatus.PAYMENT_IS_PROCESSING);
        payment.setDescription(PaymentStatus.PAYMENT_IS_PROCESSING.getDescription());
        log.info("Fill payment details: finished: payment object was filled");

        return payment;
    }
}
