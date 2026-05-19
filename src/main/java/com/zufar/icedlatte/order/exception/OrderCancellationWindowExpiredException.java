package com.zufar.icedlatte.order.exception;

import java.util.UUID;

public final class OrderCancellationWindowExpiredException extends OrderException {

    public OrderCancellationWindowExpiredException(UUID orderId) {
        super(String.format("Cancellation window has expired for order '%s'.", orderId));
    }
}
