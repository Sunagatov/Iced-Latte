package com.zufar.icedlatte.cart.exception;

import lombok.Getter;

@Getter
public class InvalidItemProductQuantityException extends RuntimeException {

    private final Integer itemProductQuantity;

    public InvalidItemProductQuantityException(final Integer itemProductQuantity) {
        super(String.format("Invalid product quantity = %s or product quantity without changes", itemProductQuantity));
        this.itemProductQuantity = itemProductQuantity;
    }
}
