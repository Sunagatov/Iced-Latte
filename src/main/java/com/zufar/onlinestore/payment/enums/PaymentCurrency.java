package com.zufar.onlinestore.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentCurrency {
    USD("usd");

    private final String currency;

    public static final String CURRENCY = "usd";
}
