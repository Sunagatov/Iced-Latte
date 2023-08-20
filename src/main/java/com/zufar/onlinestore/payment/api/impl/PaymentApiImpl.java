package com.zufar.onlinestore.payment.api.impl;

import com.stripe.exception.StripeException;
import com.zufar.onlinestore.payment.api.PaymentApi;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentDetailsDto;
import com.zufar.onlinestore.payment.api.dto.ProcessedPaymentWithClientSecretDto;
import com.zufar.onlinestore.payment.api.impl.event.PaymentEventProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.CardDetailsProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentProcessor;
import com.zufar.onlinestore.payment.api.impl.intent.PaymentRetriever;
import com.zufar.onlinestore.payment.exception.PaymentEventParsingException;
import com.zufar.onlinestore.payment.exception.PaymentEventProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentIntentProcessingException;
import com.zufar.onlinestore.payment.exception.PaymentNotFoundException;
import com.zufar.onlinestore.payment.exception.PaymentMethodProcessingException;
import com.zufar.onlinestore.payment.exception.StripeCustomerProcessingException;
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
    public ProcessedPaymentWithClientSecretDto processPayment(final String cardDetailsTokenId) throws PaymentMethodProcessingException, StripeCustomerProcessingException, PaymentIntentProcessingException {
        return paymentProcessor.processPayment(cardDetailsTokenId);
    }

    @Override
    public ProcessedPaymentDetailsDto getPaymentDetails(final Long paymentId) throws PaymentNotFoundException {
        return paymentRetriever.getPaymentDetails(paymentId);
    }

    @Override
    public void processPaymentEvent(final String paymentIntentPayload, final String stripeSignatureHeader) throws PaymentEventProcessingException, PaymentEventParsingException {
        paymentEventProcessor.processPaymentEvent(paymentIntentPayload, stripeSignatureHeader);
    }

    @Override
    public String processCardDetailsToken(CreateCardDetailsTokenDto createCardDetailsTokenDto) throws StripeException {
        return cardDetailsProcessor.processCardDetails(createCardDetailsTokenDto);
    }
}
