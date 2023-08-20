package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.Token;
import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.converter.StripePaymentMethodConverter;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import com.zufar.onlinestore.user.entity.UserEntity;
import com.zufar.onlinestore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * This class is responsible for converting passed parameters and creating based
 * on their payment method (stripe object). Payment method is secondary object for
 * creating and processing payment by Stripe API.
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class StripePaymentMethodCreator {

    private final StripePaymentMethodConverter stripePaymentMethodConverter;

    public PaymentMethod createStripePaymentMethod(final String cardDetailsTokenId) throws PaymentMethodProcessingException {
        log.info("Create stripe payment method: starting: creation stripe payment method with cardDetailsTokenId = {}.", cardDetailsTokenId);
        String paymentMethodType = Strings.EMPTY;
        try {
            Token retrievedToken = Token.retrieve(cardDetailsTokenId);
            log.info("Retrieved card details token: successfully: retrieved token with cardDetailsTokenId = {}.", cardDetailsTokenId);
            PaymentMethodCreateParams paymentMethodCreateParams = stripePaymentMethodConverter.toStripeObject(retrievedToken);
            paymentMethodType = paymentMethodCreateParams.getType().getValue();
            log.info("Create payment method: in progress: creation stripe payment method with type = {}.", paymentMethodType);

            PaymentMethod paymentMethod = PaymentMethod.create(paymentMethodCreateParams);
            String paymentMethodId = paymentMethod.getId();
            log.info("Create payment method: finished: stripe payment method was created with paymentMethodId = {}.", paymentMethodId);

            return paymentMethod;
        } catch (StripeException ex) {
            log.info("Create payment method: failed: stripe payment method was not created.");
            throw new PaymentMethodProcessingException(paymentMethodType);
        }
    }
}
