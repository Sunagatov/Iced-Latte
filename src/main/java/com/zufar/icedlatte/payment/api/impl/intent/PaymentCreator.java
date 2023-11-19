package com.zufar.icedlatte.payment.api.impl.intent;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.zufar.icedlatte.cart.api.CartApi;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.payment.converter.StripePaymentIntentConverter;
import com.zufar.icedlatte.payment.entity.Payment;
import com.zufar.icedlatte.payment.enums.PaymentStatus;
import com.zufar.icedlatte.payment.exception.ShoppingCartAlreadyPaidException;
import com.zufar.icedlatte.payment.repository.PaymentRepository;
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

        ShoppingCartDto shoppingCart = cartApi.getShoppingCartByUserId(userId);
        PaymentIntent stripePaymentIntent = stripePaymentIntentCreator.createStripePaymentIntent(paymentMethod, shoppingCart);
        Payment paymentToSave = fillPaymentDetails(shoppingCart, stripePaymentIntent);
        try {
            Payment savedPayment = paymentRepository.save(paymentToSave);
            log.info("Create payment: finishing: payment was created");

            return Pair.of(stripePaymentIntent.getClientSecret(), savedPayment);

        } catch (DataIntegrityViolationException e) {
           throw new ShoppingCartAlreadyPaidException(shoppingCart.getId());
        }
    }

    private Payment fillPaymentDetails(ShoppingCartDto shoppingCart, PaymentIntent stripePaymentIntent) {
        log.info("Fill payment details: starting: start payment object filling");
        Payment payment = stripePaymentIntentConverter.toEntity(stripePaymentIntent, shoppingCart);
        payment.setStatus(PaymentStatus.PAYMENT_IS_PROCESSING);
        payment.setDescription(PaymentStatus.PAYMENT_IS_PROCESSING.getDescription());
        log.info("Fill payment details: finished: payment object was filled");

        return payment;
    }
}
