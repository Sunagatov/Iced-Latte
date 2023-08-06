package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.zufar.onlinestore.cart.api.CartApi;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.api.dto.ProcessPaymentDto;
import com.zufar.onlinestore.payment.api.impl.customer.StripeCustomerCreator;
import com.zufar.onlinestore.payment.entity.Payment;
import com.zufar.onlinestore.payment.exception.PaymentIntentProcessingException;
import com.zufar.onlinestore.payment.exception.StripeCustomerCreationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentProcessor {

    private final CartApi cartApi;
    private final StripeCustomerCreator stripeCustomerCreator;
    private final StripePaymentMethodCreator stripePaymentMethodCreator;
    private final StripePaymentIntentCreator stripePaymentIntentCreator;
    private final PaymentCreator paymentCreator;

    public ProcessedPaymentWithClientSecretDto processPayment(final ProcessPaymentDto processPaymentDto) throws StripeCustomerCreationException, StripeCustomerCreationException, PaymentIntentProcessingException {
        log.info("Process payment: starting: processing payment with card info token: {}.", processPaymentDto.cardInfoToken());
        UUID customerId = processPaymentDto.customerId();
        Customer stripeCustomer = stripeCustomerCreator.createStripeCustomer(customerId);
        ShoppingSessionDto shoppingSession = cartApi.getShoppingSessionByUserId(customerId);
        PaymentMethod stripePaymentMethod = stripePaymentMethodCreator.createStripePaymentMethod(processPaymentDto.cardInfoToken());
        stripePaymentMethod.setCustomer(stripeCustomer.getId());
        PaymentIntent paymentIntent = stripePaymentIntentCreator.createStripePaymentIntent(stripePaymentMethod, shoppingSession);
        Payment savedPayment = paymentCreator.createPayment(paymentIntent, shoppingSession);
        log.info("Process payment: finishing: payment was processed with id: {}.", savedPayment.getPaymentId());

        return ProcessedPaymentWithClientSecretDto.builder()
                .paymentId(savedPayment.getPaymentId())
                .clientSecret(paymentIntent.getClientSecret())
                .build();
    }

}
