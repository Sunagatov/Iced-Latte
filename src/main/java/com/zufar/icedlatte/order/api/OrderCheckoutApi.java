package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.order.api.dto.CheckoutOrderRequest;

import java.util.UUID;

public interface OrderCheckoutApi {

    OrderSnapshot createPendingPaymentOrderSnapshot(UUID userId,
                                                    CheckoutOrderRequest request,
                                                    CartSnapshot cart);
}
