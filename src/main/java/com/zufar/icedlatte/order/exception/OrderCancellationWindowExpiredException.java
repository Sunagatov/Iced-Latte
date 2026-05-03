package com.zufar.icedlatte.order.exception;

import java.util.UUID;

public class OrderCancellationWindowExpiredException extends RuntimeException {

    public OrderCancellationWindowExpiredException(UUID orderId) {
        super(String.format("Cancellation window has expired for order '%s'.", orderId));
    }
}
