package com.zufar.icedlatte.payment.calculator;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class PaymentPriceCalculator {

    private static final Double COIN_TO_CURRENCY_CONVERSION_VALUE = 100.0;

    @Named("calculateForPayment")
    public BigDecimal calculatePriceForPayment(Long totalPrice) {
        return BigDecimal.valueOf(totalPrice / COIN_TO_CURRENCY_CONVERSION_VALUE);
    }

    @Named("calculateForPaymentIntent")
    public Long calculatePriceForPaymentIntent(BigDecimal totalPrice) {
        return totalPrice.multiply(BigDecimal.valueOf(COIN_TO_CURRENCY_CONVERSION_VALUE)).longValue();
    }
}
