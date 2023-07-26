package com.zufar.onlinestore.payment.api.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.dto.*;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.service.PaymentCreator;
import com.zufar.onlinestore.payment.service.PaymentEventProcessor;
import com.zufar.onlinestore.payment.service.PaymentMethodCreator;
import com.zufar.onlinestore.payment.service.PaymentRetriever;
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
    public PaymentDetailsWithTokenDto createPayment(final CreatePaymentDto createPaymentDto) throws StripeException {
        return paymentCreator.createPayment(createPaymentDto);
    }

    @Override
    public String createPaymentMethod(final CreatePaymentMethodDto createPaymentMethodDto) throws StripeException {
        return paymentMethodCreator.createPaymentMethod(createPaymentMethodDto);
    }

    @Override
    public PaymentDetailsDto getPaymentDetails(Long paymentId) throws PaymentNotFoundException {
        return paymentRetriever.getPaymentDetails(paymentId);
    }

    @Override
    public void paymentEventProcess(String paymentIntentPayload, String stripeSignatureHeader) throws SignatureVerificationException {
        paymentEventProcessor.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
    }
}
