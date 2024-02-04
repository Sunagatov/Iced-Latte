package com.zufar.icedlatte.payment.converter;

import com.stripe.model.Token;
import com.stripe.param.PaymentMethodCreateParams;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentMethodConverter {

    public PaymentMethodCreateParams toStripeObject(Token token) {
        if ( token == null ) {
            return null;
        }

        PaymentMethodCreateParams.Builder paymentMethodCreateParams = PaymentMethodCreateParams.builder();

        paymentMethodCreateParams.setType( toType( token ) );
        paymentMethodCreateParams.setCard( toToken( token.getId() ) );

        return paymentMethodCreateParams.build();
    }

    private PaymentMethodCreateParams.Token toToken(String tokenId) {
        return PaymentMethodCreateParams.Token.builder()
                .setToken(tokenId)
                .build();
    }

    private PaymentMethodCreateParams.Type toType(Token token) {
        return PaymentMethodCreateParams.Type.valueOf(token.getType().toUpperCase());
    }
}
