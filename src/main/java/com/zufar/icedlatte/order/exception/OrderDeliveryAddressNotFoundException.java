package com.zufar.icedlatte.order.exception;

public final class OrderDeliveryAddressNotFoundException extends OrderException {

    public OrderDeliveryAddressNotFoundException() {
        super("Delivery address not found.");
    }
}
