package com.zufar.icedlatte.user.exception;

import java.util.UUID;

public class DeliveryAddressNotFoundException extends RuntimeException {

    public DeliveryAddressNotFoundException(UUID addressId) {
        super(String.format("Delivery address with id = %s is not found.", addressId));
    }
}
