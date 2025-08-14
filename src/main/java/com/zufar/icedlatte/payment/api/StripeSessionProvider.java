package com.zufar.icedlatte.payment.api;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.zufar.icedlatte.payment.exception.StripeSessionRetrievalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class StripeSessionProvider {

    public Session get(String sessionId) throws StripeSessionRetrievalException {
        Session session;
        try {
            session = Session.retrieve(sessionId);
        } catch (StripeException e) {
            throw new StripeSessionRetrievalException(e.getMessage(), sessionId, e);
        }
        return session;
    }
}
