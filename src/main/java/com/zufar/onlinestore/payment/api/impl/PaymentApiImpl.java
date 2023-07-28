package com.zufar.onlinestore.payment.api.impl;

import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.dto.*;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentCreator;
import com.zufar.onlinestore.payment.api.impl.event.PaymentEventProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentMethodCreator;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApiImpl implements PaymentApi {

    private final PaymentRetriever paymentRetriever;
    private final PaymentCreator paymentCreator;
    private final PaymentMethodCreator paymentMethodCreator;
    private final PaymentEventProcessor paymentEventProcessor;

    @Override
    public PaymentDetailsWithTokenDto createPayment(final CreatePaymentDto createPaymentDto) {
        return paymentCreator.createPayment(createPaymentDto);
    }

    @Override
    public String createPaymentMethod(final CreatePaymentMethodDto createPaymentMethodDto) {
        return paymentMethodCreator.createPaymentMethod(createPaymentMethodDto);
    }

    @Override
    public PaymentDetailsDto getPaymentDetails(final Long paymentId) {
        return paymentRetriever.getPaymentDetails(paymentId);
    }

    @Override
    public void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) {
        paymentEventProcessor.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
    }
}
