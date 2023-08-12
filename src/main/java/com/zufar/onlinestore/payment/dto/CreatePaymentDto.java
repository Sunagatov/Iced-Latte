package com.zufar.onlinestore.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentDto(

        @NotBlank(message = "PaymentMethodId is the mandatory attribute")
        String paymentMethodId,

        @NotNull(message = "ItemsTotalPrice is mandatory attribute")
        BigDecimal itemsTotalPrice
) {
}
