package com.zufar.icedlatte.order.exception;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;

public final class InvalidOrderStateTransitionException extends OrderException {

    public InvalidOrderStateTransitionException(OrderStatus currentStatus, OrderEvent event) {
        super(String.format("Cannot apply event '%s' to order in status '%s'.", event, currentStatus));
    }
}
