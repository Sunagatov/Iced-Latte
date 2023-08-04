package com.zufar.onlinestore.payment.converter;

import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodConverter {

    public PaymentMethodCreateParams toPaymentMethodParams(final CreatePaymentMethodDto createPaymentMethodDto) {
        PaymentMethodCreateParams.CardDetails cardDetails = PaymentMethodCreateParams.CardDetails.builder()
                .setNumber(createPaymentMethodDto.cardNumber())
                .setExpMonth(createPaymentMethodDto.expMonth())
                .setExpYear(createPaymentMethodDto.expYear())
                .setCvc(createPaymentMethodDto.cvc())
                .build();

        return PaymentMethodCreateParams.builder()
                .setCard(cardDetails)
                .setType(PaymentMethodCreateParams.Type.CARD)
                .build();
    }
}
