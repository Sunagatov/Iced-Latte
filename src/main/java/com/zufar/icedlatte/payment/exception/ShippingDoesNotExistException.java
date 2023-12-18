package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

@Getter
public class ShippingDoesNotExistException extends RuntimeException {

    private final Long shippingId;

    public ShippingDoesNotExistException(Long shippingId) {
        super(String.format("Shipping does not exist by the following ID: %d", shippingId));
        this.shippingId = shippingId;
    }
}
