package com.zufar.icedlatte.order.api;

import java.util.UUID;

import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.order.api.dto.CheckoutOrderRequest;

public interface OrderCheckoutApi {

    OrderSnapshot createPendingPaymentOrderSnapshot(UUID userId, CheckoutOrderRequest request, CartSnapshot cart);
}
