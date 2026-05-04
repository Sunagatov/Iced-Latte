package com.zufar.icedlatte.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("Order not found.");
    }

    public OrderNotFoundException(Object orderId) {
        super("Order not found: " + orderId);
    }
}
