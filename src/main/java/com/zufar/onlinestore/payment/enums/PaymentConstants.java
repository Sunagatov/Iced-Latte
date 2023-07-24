package com.zufar.onlinestore.payment.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentConstants {

    PAYMENT_DELIMITER(100);

    private final Integer value;

}
