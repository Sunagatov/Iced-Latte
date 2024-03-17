package com.zufar.icedlatte.payment.calculator;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Slf4j
@Component
public class PaymentPriceCalculator {
    private static final Double COIN_TO_CURRENCY_CONVERSION_VALUE = 100.0;
    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.05");
    public static final BigDecimal DEFAULT_SHIPPING_COST = new BigDecimal("5.00");

    @Named("calculateForPayment")
    public BigDecimal calculatePriceForPayment(Long totalPrice) {
        BigDecimal priceInDollars = BigDecimal.valueOf(totalPrice / COIN_TO_CURRENCY_CONVERSION_VALUE);
        BigDecimal taxAmount = priceInDollars.multiply(DEFAULT_TAX_RATE);
        return priceInDollars.add(taxAmount).add(DEFAULT_SHIPPING_COST);
    }

    @Named("calculateForPaymentIntent")
    public Long calculatePriceForPaymentIntent(BigDecimal totalPrice) {
        BigDecimal totalWithTaxAndShipping = totalPrice.add(totalPrice.multiply(DEFAULT_TAX_RATE)).add(DEFAULT_SHIPPING_COST);
        return totalWithTaxAndShipping.multiply(BigDecimal.valueOf(COIN_TO_CURRENCY_CONVERSION_VALUE)).longValue();
    }
}
