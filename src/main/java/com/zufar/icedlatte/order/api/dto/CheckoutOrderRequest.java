package com.zufar.icedlatte.order.api.dto;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record CheckoutOrderRequest(@Nullable String recipientName,
                                   @Nullable String recipientSurname,
                                   @Nullable String recipientPhone,
                                   @Nullable UUID deliveryAddressId,
                                   @Nullable OrderAddressRequest address) {
}
