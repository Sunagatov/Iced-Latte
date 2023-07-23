package com.zufar.onlinestore.payment.service;

import com.stripe.exception.SignatureVerificationException;

public interface PaymentEventService {

    void processPaymentEvents(String payload, String header) throws SignatureVerificationException;

}
