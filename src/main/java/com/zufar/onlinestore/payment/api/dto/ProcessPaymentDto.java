package com.zufar.onlinestore.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ProcessPaymentDto(

        @NotBlank(message = "CardInfoToken is the mandatory attribute")
        String cardInfoToken,

        @NotBlank(message = "CustomerId is the mandatory attribute")
        UUID customerId
) {
}
