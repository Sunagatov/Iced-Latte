package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.api.impl.customer.StripeCustomerProcessor;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.exception.PaymentIntentProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import com.zufar.onlinestore.payment.exception.StripeCustomerProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentProcessor {

    private final StripeCustomerProcessor stripeCustomerProcessor;
    private final StripePaymentMethodCreator stripePaymentMethodCreator;
    private final StripePaymentIntentCreator stripePaymentIntentCreator;
    private final StripeConfiguration stripeConfiguration;
    private final PaymentCreator paymentCreator;
    private final CartApi cartApi;

    public ProcessedPaymentWithClientSecretDto processPayment(final String cardDetailsTokenId) throws PaymentMethodProcessingException, StripeCustomerProcessingException, PaymentIntentProcessingException {
        log.info("Process payment: starting: processing payment with cardDetailsTokenId = {}.", cardDetailsTokenId);
        StripeConfiguration.setStripeKey(stripeConfiguration.secretKey());

        Customer stripeCustomer = stripeCustomerProcessor.processStripeCustomer();
        PaymentMethod stripePaymentMethod = stripePaymentMethodCreator.createStripePaymentMethod(cardDetailsTokenId);
        String authorizedUserId = stripeCustomer.getMetadata().get("authorizedUserId");
        ShoppingSessionDto shoppingSession = cartApi.getShoppingSessionByUserId(UUID.fromString(authorizedUserId));
        stripePaymentMethod.setCustomer(stripeCustomer.getId());
        PaymentIntent paymentIntent = stripePaymentIntentCreator.createStripePaymentIntent(stripePaymentMethod, shoppingSession);
        Payment savedPayment = paymentCreator.createPayment(paymentIntent, shoppingSession);
        log.info("Process payment: finishing: payment was processed with id = {}.", savedPayment.getPaymentId());

        return ProcessedPaymentWithClientSecretDto.builder()
                .paymentId(savedPayment.getPaymentId())
                .clientSecret(paymentIntent.getClientSecret())
                .build();
    }
}
