package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.CreateCheckoutRequestDto;
import com.zufar.icedlatte.openapi.dto.CreateNewOrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;

import java.util.UUID;

public interface OrderCheckoutApi {

    OrderDto create(UUID userId, CreateNewOrderRequestDto request, String idempotencyKey);

    OrderSnapshot createPendingPaymentOrderSnapshot(UUID userId, CreateCheckoutRequestDto request, ShoppingCartDto cart);
}
