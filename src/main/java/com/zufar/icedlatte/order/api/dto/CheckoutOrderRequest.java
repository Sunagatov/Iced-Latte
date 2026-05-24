package com.zufar.icedlatte.order.api.dto;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

public record CheckoutOrderRequest(
        @Nullable String recipientName,
        @Nullable String recipientSurname,
        @Nullable String recipientPhone,
        @Nullable UUID deliveryAddressId,
        @Nullable OrderAddressRequest address) {}
