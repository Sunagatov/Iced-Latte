package com.zufar.onlinestore.payment.api.impl;

import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenRequest;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.api.impl.event.PaymentEventProcessor;
import com.zufar.onlinestore.payment.api.impl.customer.CardDetailsProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApiImpl implements PaymentApi {

    private final PaymentRetriever paymentRetriever;
    private final PaymentProcessor paymentProcessor;
    private final PaymentEventProcessor paymentEventProcessor;
    private final CardDetailsProcessor cardDetailsProcessor;

    @Override
    public ProcessedPaymentWithClientSecretDto processPayment(final String cardDetailsTokenId) {
        return paymentProcessor.processPayment(cardDetailsTokenId);
    }

    @Override
    public ProcessedPaymentDetailsDto getPaymentDetails(final Long paymentId) {
        return paymentRetriever.getPaymentDetails(paymentId);
    }

    @Override
    public void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) {
        paymentEventProcessor.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
    }

    @Override
    public String processCardDetailsToken(CreateCardDetailsTokenRequest createCardDetailsTokenRequest) {
        return cardDetailsProcessor.processCardDetails(createCardDetailsTokenRequest);
    }
}
