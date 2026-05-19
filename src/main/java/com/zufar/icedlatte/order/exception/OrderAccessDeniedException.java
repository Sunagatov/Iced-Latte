package com.zufar.icedlatte.order.exception;

public final class OrderAccessDeniedException extends OrderException {

    public OrderAccessDeniedException() {
        super("Access denied.");
    }
}
