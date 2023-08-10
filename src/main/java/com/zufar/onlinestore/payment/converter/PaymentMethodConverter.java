package com.zufar.onlinestore.payment.converter;

import com.stripe.param.PaymentMethodCreateParams;
import com.zufar.onlinestore.payment.dto.CreatePaymentMethodDto;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodConverter {

    public PaymentMethodCreateParams toPaymentMethodParams(final CreatePaymentMethodDto createPaymentMethodDto) {
        PaymentMethodCreateParams.Builder paymentMethod = PaymentMethodCreateParams.builder();
        PaymentMethodCreateParams.CardDetails cardDetails = getCardDetails(createPaymentMethodDto);

        paymentMethod.setCard(cardDetails);
        paymentMethod.setType(PaymentMethodCreateParams.Type.CARD);

        return paymentMethod.build();
    }

    private PaymentMethodCreateParams.CardDetails getCardDetails(final CreatePaymentMethodDto createPaymentMethodDto) {
        PaymentMethodCreateParams.CardDetails.Builder cardDetails = PaymentMethodCreateParams.CardDetails.builder();

        cardDetails.setNumber(createPaymentMethodDto.cardNumber());
        cardDetails.setExpYear(createPaymentMethodDto.expYear());
        cardDetails.setExpMonth(createPaymentMethodDto.expMonth());
        cardDetails.setCvc(createPaymentMethodDto.cvc());

        return cardDetails.build();
    }
}
