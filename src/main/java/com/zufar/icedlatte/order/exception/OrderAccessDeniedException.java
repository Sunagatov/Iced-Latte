package com.zufar.icedlatte.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public final class OrderAccessDeniedException extends OrderException {

    public OrderAccessDeniedException() {
        super("Access denied.");
    }
}
