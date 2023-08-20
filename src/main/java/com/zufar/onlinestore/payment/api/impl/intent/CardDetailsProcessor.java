package com.zufar.onlinestore.payment.api.impl.intent;

import com.stripe.exception.StripeException;
import com.stripe.model.Token;
import com.zufar.onlinestore.payment.api.dto.CreateCardDetailsTokenDto;
import com.zufar.onlinestore.payment.config.StripeConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Deprecated
public class CardDetailsProcessor {

    private final StripeConfiguration stripeConfiguration;

    public String processCardDetails(CreateCardDetailsTokenDto createCardDetailsTokenDto) throws StripeException {
        StripeConfiguration.setStripeKey(stripeConfiguration.publishableKey());

        Map<String, Object> card = new HashMap<>();
        card.put("number", createCardDetailsTokenDto.cardNumber());
        card.put("exp_month", Integer.parseInt(createCardDetailsTokenDto.expMonth()));
        card.put("exp_year", Integer.parseInt(createCardDetailsTokenDto.expYear()));
        card.put("cvc", createCardDetailsTokenDto.cvc());
        Map<String, Object> params = new HashMap<>();
        params.put("card", card);

        Token cardDetailsToken = Token.create(params);
        return cardDetailsToken.getId();
    }
}
