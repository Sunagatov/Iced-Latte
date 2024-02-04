package com.zufar.icedlatte.payment.converter;

import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentIntentCreateParams;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.payment.calculator.PaymentPriceCalculator;
import com.zufar.icedlatte.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentIntentConverter {

    private final PaymentPriceCalculator paymentPriceCalculator;

    public Payment toEntity(final PaymentIntent paymentIntent, final ShoppingCartDto shoppingCart) {
        if ( paymentIntent == null && shoppingCart == null ) {
            return null;
        }

        Payment.PaymentBuilder payment = Payment.builder();

        if ( paymentIntent != null ) {
            payment.paymentIntentId( paymentIntent.getId() );
            payment.itemsTotalPrice( paymentPriceCalculator.calculatePriceForPayment( paymentIntent.getAmount() ) );
        }
        if ( shoppingCart != null ) {
            payment.shoppingCartId( shoppingCart.getId() );
        }

        return payment.build();
    }

    public PaymentIntentCreateParams toStripeObject(final PaymentMethod paymentMethod,
                                             final ShoppingCartDto shoppingCart, final String currency) {
        if ( paymentMethod == null && shoppingCart == null && currency == null ) {
            return null;
        }

        PaymentIntentCreateParams.Builder paymentIntentCreateParams = PaymentIntentCreateParams.builder();

        if ( paymentMethod != null ) {
            paymentIntentCreateParams.setCustomer( paymentMethod.getCustomer() );
            paymentIntentCreateParams.setPaymentMethod( paymentMethod.getId() );
            paymentIntentCreateParams.setRadarOptions( radarOptionsToRadarOptions( paymentMethod.getRadarOptions() ) );
        }
        if ( shoppingCart != null ) {
            paymentIntentCreateParams.setAmount( paymentPriceCalculator.calculatePriceForPaymentIntent( shoppingCart.getItemsTotalPrice() ) );
        }
        paymentIntentCreateParams.setCurrency( currency );

        return paymentIntentCreateParams.build();
    }

    protected PaymentIntentCreateParams.RadarOptions radarOptionsToRadarOptions(PaymentMethod.RadarOptions radarOptions) {
        if ( radarOptions == null ) {
            return null;
        }

        PaymentIntentCreateParams.RadarOptions.Builder radarOptions1 = PaymentIntentCreateParams.RadarOptions.builder();

        radarOptions1.setSession( radarOptions.getSession() );

        return radarOptions1.build();
    }
}
