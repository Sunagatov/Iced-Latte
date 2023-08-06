package com.zufar.onlinestore.payment.api.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.dto.*;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentCreator;
import com.zufar.onlinestore.payment.api.impl.event.PaymentEventProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentMethodCreator;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentRetriever;
import com.zufar.onlinestore.payment.exception.*;
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
    public PaymentDetailsWithTokenDto createPayment(final CreatePaymentDto createPaymentDto) throws PaymentIntentProcessingException {
        return paymentCreator.createPayment(createPaymentDto);
    }

    @Override
    public String createPaymentMethod(final CreatePaymentMethodDto createPaymentMethodDto) throws PaymentMethodProcessingException {
        return paymentMethodCreator.createPaymentMethod(createPaymentMethodDto);
    }

    @Override
    public PaymentDetailsDto getPaymentDetails(Long paymentId) throws PaymentNotFoundException {
        return paymentRetriever.getPaymentDetails(paymentId);
    }

    @Override
    public void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) throws PaymentEventProcessingException, PaymentEventParsingException {
        paymentEventProcessor.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
    }
}
