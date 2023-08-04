package com.zufar.onlinestore.payment.converter;

import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import org.mapstruct.Mapper;

@Mapper
public interface PaymentMethodConverter {

    default PaymentMethodCreateParams toPaymentMethodParams(final CreatePaymentMethodDto createPaymentMethodDto) {
        if (createPaymentMethodDto == null) {
            return null;
        }

        PaymentMethodCreateParams.CardDetails.Builder cardDetails = PaymentMethodCreateParams.CardDetails.builder();
        cardDetails.setNumber(createPaymentMethodDto.cardNumber());
        cardDetails.setExpMonth(createPaymentMethodDto.expMonth());
        cardDetails.setExpYear(createPaymentMethodDto.expYear());
        cardDetails.setCvc(createPaymentMethodDto.cvc());

        PaymentMethodCreateParams.Builder builder = PaymentMethodCreateParams.builder();

        builder.setCard(cardDetails.build());
        builder.setType(PaymentMethodCreateParams.Type.CARD);

        return builder.build();
    }
}
