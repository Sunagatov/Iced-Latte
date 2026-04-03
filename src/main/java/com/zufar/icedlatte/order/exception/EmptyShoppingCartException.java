package com.zufar.icedlatte.order.exception;

import java.util.UUID;

public class EmptyShoppingCartException extends RuntimeException {

    public EmptyShoppingCartException(final UUID userId) {
        super(String.format("Cannot create order: shopping cart is empty for userId=%s.", userId));
    }
}
