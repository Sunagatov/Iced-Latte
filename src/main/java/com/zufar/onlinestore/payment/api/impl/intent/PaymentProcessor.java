package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.model.PaymentMethod;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.api.impl.customer.StripeCustomerDataProcessor;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentProcessor {

    private final StripeCustomerDataProcessor stripeCustomerDataProcessor;
    private final StripeConfiguration stripeConfiguration;
    private final PaymentCreator paymentCreator;

    public ProcessedPaymentWithClientSecretDto processPayment(final String cardDetailsTokenId) {
        log.info("Process payment: starting: processing payment with cardDetailsTokenId = {}.", cardDetailsTokenId);
        StripeConfiguration.setStripeKey(stripeConfiguration.secretKey());

        Pair<UUID, PaymentMethod> userIdAndPaymentMethodPair = stripeCustomerDataProcessor.processStripeCustomerData(cardDetailsTokenId);
        Pair<String, Payment> clientSecretAndPaymentPair = paymentCreator.createPayment(userIdAndPaymentMethodPair);
        String clientSecret = clientSecretAndPaymentPair.getLeft();
        Payment payment = clientSecretAndPaymentPair.getRight();

        Long paymentId = payment.getPaymentId();
        log.info("Process payment: finishing: payment was processed with paymentId = {}.", paymentId);

        ProcessedPaymentWithClientSecretDto processedPaymentWithClientSecretDto = new ProcessedPaymentWithClientSecretDto();
        processedPaymentWithClientSecretDto.setPaymentId(paymentId);
        processedPaymentWithClientSecretDto.setClientSecret(clientSecret);

        return processedPaymentWithClientSecretDto;
    }
}
