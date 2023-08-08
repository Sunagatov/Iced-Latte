package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.converter.PaymentMethodConverter;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for converting passed parameters and creating based
 * on their payment method (stripe object). Payment method is secondary object for
 * creating and processing payment by Stripe API.
 * */

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentMethodCreator {

    private final StripeConfiguration stripeConfig;
    private final PaymentMethodConverter paymentMethodConverter;

    public String createPaymentMethod(final CreatePaymentMethodDto createPaymentMethodDto) throws PaymentMethodProcessingException{
        Stripe.apiKey = stripeConfig.publishableKey();
        PaymentMethod paymentMethod;
        PaymentMethodCreateParams params = paymentMethodConverter.toPaymentMethodParams(createPaymentMethodDto);
        try {
            paymentMethod = PaymentMethod.create(params);
        } catch (StripeException ex) {
            log.error("Error during payment method processing", ex);
            throw new PaymentMethodProcessingException(params.getType().getValue());
        }
        log.info("Created payment method: payment method has been created: paymentMethod: {}.", paymentMethod);

        return paymentMethod.getId();
    }
}
