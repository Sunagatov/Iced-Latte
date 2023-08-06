package com.zufar.onlinestore.payment.api.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.api.dto.ProcessPaymentDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.api.impl.event.PaymentEventProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentRetriever;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
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

    @Override
    public ProcessedPaymentWithClientSecretDto processPayment(final ProcessPaymentDto processPaymentDto) throws StripeException {
        return paymentProcessor.processPayment(processPaymentDto);
    }

    @Override
    public ProcessedPaymentDetailsDto getPaymentDetails(Long paymentId) throws PaymentNotFoundException {
        return paymentRetriever.getPaymentDetails(paymentId);
    }

    @Override
    public void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) throws SignatureVerificationException {
        paymentEventProcessor.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
    }
}
