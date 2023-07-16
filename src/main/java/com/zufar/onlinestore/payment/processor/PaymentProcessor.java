package com.zufar.onlinestore.payment.processor;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.config.StripeTemplate;
import com.zufar.onlinestore.payment.model.Payment;
import com.zufar.onlinestore.payment.model.PaymentDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentProcessor {

    public static final String PAYMENT_MESSAGE = "Payment made by user: %s, using the payment method: %s.";
    private final StripeTemplate stripeTemplate;


    public PaymentDetails process(String paymentMethodId, BigDecimal totalPrice, String currency) throws StripeException {
        Stripe.apiKey = stripeTemplate.secretKey();

        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
        PaymentIntentCreateParams params = getPaymentParams(paymentMethod, totalPrice, currency);
        log.debug("Params for Payment Intent create {}.", params);
        PaymentIntent paymentIntent = PaymentIntent.create(params);
        log.debug("Payment Intent created with id {}.", paymentIntent.getId());

        return PaymentDetails.builder()
                .paymentToken(paymentIntent.getClientSecret())
                .payment(getProcessedPayment(paymentIntent))
                .build();
    }

    private static PaymentIntentCreateParams getPaymentParams(PaymentMethod paymentMethod, BigDecimal totalPrice, String currency) {
        String email = paymentMethod.getBillingDetails().getEmail();
        return PaymentIntentCreateParams.builder()
                .setAmount(totalPrice.longValue() * 100)
                .setCurrency(currency)
                .setPaymentMethod(paymentMethod.getId())
                .setReceiptEmail(email)
                .setDescription(PAYMENT_MESSAGE.formatted(email, paymentMethod.getType()))
                .build();
    }

    private Payment getProcessedPayment(PaymentIntent paymentIntent) {
        return Payment.builder()
                .totalPrice(BigDecimal.valueOf(paymentIntent.getAmount()))
                .currency(paymentIntent.getCurrency())
                .description(paymentIntent.getDescription())
                .status(paymentIntent.getStatus())
                .build();
    }

}
