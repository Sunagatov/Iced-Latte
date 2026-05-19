package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

@Getter
public final class InvalidItemProductQuantityException extends CartException {

    private final Integer itemProductQuantity;

    public InvalidItemProductQuantityException(final Integer itemProductQuantity,
                                               final int maxItemProductQuantity) {
        super(String.format(
                "Product quantity must be between 1 and %s. Actual value = %s.",
                maxItemProductQuantity,
                itemProductQuantity));
        this.itemProductQuantity = itemProductQuantity;
    }

    public InvalidItemProductQuantityException(final String message) {
        super(message);
        this.itemProductQuantity = null;
    }
}
