package com.zufar.onlinestore.payment.processor;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.onlinestore.payment.exception.PaymentProcessingException;
import com.zufar.onlinestore.payment.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
@Component
public class PaymentProcessor {

    public static final String PAYMENT_MESSAGE = "Payment made by user: %s, using the payment method: %s.";
    public static final Integer PAYMENT_DELIMITER = 100;

    public Pair<String, Payment> process(String paymentMethodId, BigDecimal totalPrice, String currency) {
        PaymentIntent paymentIntent;
        try {
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            PaymentIntentCreateParams params = getPaymentParams(paymentMethod, totalPrice, currency);
            log.info("process: get payment intent params for payment creation: params: {}.", params);
            paymentIntent = PaymentIntent.create(params);
        } catch (StripeException e) {
            log.error("Error during Payment processing", e);
            throw new PaymentProcessingException(paymentMethodId);
        }
        log.info("process: payment intent successfully created: paymentIntentId: {}.", paymentIntent.getId());
        String paymentToken = paymentIntent.getClientSecret();
        Payment payment = getProcessedPayment(paymentIntent);

        return Pair.of(paymentToken, payment);
    }

    private PaymentIntentCreateParams getPaymentParams(PaymentMethod paymentMethod, BigDecimal totalPrice, String currency) {
        String email = paymentMethod.getBillingDetails().getEmail();
        return PaymentIntentCreateParams.builder()
                .setAmount(totalPrice.longValue() * PAYMENT_DELIMITER)
                .setCurrency(currency)
                .setPaymentMethod(paymentMethod.getId())
                .setReceiptEmail(email)
                .setDescription(PAYMENT_MESSAGE.formatted(email, paymentMethod.getType()))
                .build();
    }

    private Payment getProcessedPayment(PaymentIntent paymentIntent) {
        return Payment.builder()
                .itemsTotalPrice(BigDecimal.valueOf(paymentIntent.getAmount() / PAYMENT_DELIMITER))
                .currency(paymentIntent.getCurrency())
                .description(paymentIntent.getDescription())
                .status(paymentIntent.getStatus())
                .build();
    }

}