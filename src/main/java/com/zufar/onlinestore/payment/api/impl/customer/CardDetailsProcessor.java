package com.zufar.onlinestore.payment.api.impl.customer;

import com.stripe.exception.StripeException;
import com.stripe.model.Token;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenDto;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import com.zufar.onlinestore.payment.exception.CardTokenCreationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CardDetailsProcessor {

    private final StripeConfiguration stripeConfiguration;

    public String processCardDetails(CreateCardDetailsTokenDto createCardDetailsTokenDto) {
        StripeConfiguration.setStripeKey(stripeConfiguration.publishableKey());

        Map<String, Object> card = createCardDetails(createCardDetailsTokenDto);
        Token cardDetailsToken;
        try {
            cardDetailsToken = Token.create(Map.of("card", card));
        } catch (StripeException e) {
            throw new CardTokenCreationException(createCardDetailsTokenDto.cardNumber());
        }
        return cardDetailsToken.getId();
    }

    private static Map<String, Object> createCardDetails(CreateCardDetailsTokenDto createCardDetailsTokenDto) {
        return Map.of("number", createCardDetailsTokenDto.cardNumber(),
                "exp_month", Integer.parseInt(createCardDetailsTokenDto.expMonth()),
                "exp_year", Integer.parseInt(createCardDetailsTokenDto.expYear()),
                "cvc", createCardDetailsTokenDto.cvc());
    }
}
