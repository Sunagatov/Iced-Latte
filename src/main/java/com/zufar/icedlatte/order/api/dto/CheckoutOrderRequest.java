package com.zufar.icedlatte.order.api.dto;

import java.util.UUID;

public record CheckoutOrderRequest(String recipientName,
                                   String recipientSurname,
                                   String recipientPhone,
                                   UUID deliveryAddressId,
                                   OrderAddressRequest address) {
}
