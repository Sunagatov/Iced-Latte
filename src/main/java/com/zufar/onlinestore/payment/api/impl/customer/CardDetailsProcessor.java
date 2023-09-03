package com.zufar.onlinestore.payment.api.impl.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Token;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenRequest;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.exception.CardTokenCreationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CardDetailsProcessor {

    private final StripeConfiguration stripeConfiguration;

    public String processCardDetails(CreateCardDetailsTokenRequest createCardDetailsTokenRequest) {
        StripeConfiguration.setStripeKey(stripeConfiguration.publishableKey());

        Map<String, Object> card = createCardDetails(createCardDetailsTokenRequest);
        Token cardDetailsToken;
        try {
            cardDetailsToken = Token.create(Map.of("card", card));
        } catch (StripeException e) {
            throw new CardTokenCreationException(createCardDetailsTokenRequest.cardNumber());
        }
        return cardDetailsToken.getId();
    }

    private Map<String, Object> createCardDetails(CreateCardDetailsTokenRequest createCardDetailsTokenRequest) {
        return Map.of("number", createCardDetailsTokenRequest.cardNumber(),
                "exp_month", Integer.parseInt(createCardDetailsTokenRequest.expMonth()),
                "exp_year", Integer.parseInt(createCardDetailsTokenRequest.expYear()),
                "cvc", createCardDetailsTokenRequest.cvc());
    }
}
