package com.zufar.icedlatte.order.exception;

import java.util.UUID;

public class OrderAccessDeniedException extends RuntimeException {

    public OrderAccessDeniedException(UUID orderId) {
        super(String.format("Access denied to order '%s'.", orderId));
    }
}
