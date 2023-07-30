package com.zufar.onlinestore.payment.calculator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class PaymentPriceCalculator {

    private static final Long COIN_TO_CURRENCY_CONVERSION_VALUE = 100L;

    public BigDecimal calculatePriceForPayment(Long totalPrice) {
        return BigDecimal.valueOf(totalPrice / COIN_TO_CURRENCY_CONVERSION_VALUE);
    }

    public Long calculatePriceForPaymentIntent(BigDecimal totalPrice) {
        return totalPrice.longValue() * COIN_TO_CURRENCY_CONVERSION_VALUE;
    }
}
