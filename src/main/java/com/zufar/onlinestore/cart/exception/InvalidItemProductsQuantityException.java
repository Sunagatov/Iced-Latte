package com.zufar.onlinestore.cart.exception;

import lombok.Getter;

@Getter
public class InvalidItemProductsQuantityException extends RuntimeException {

    private final Integer itemProductsQuantity;

    public InvalidItemProductsQuantityException(final Integer itemProductsQuantity) {
        super(String.format("Invalid products quantity = %s", itemProductsQuantity));
        this.itemProductsQuantity = itemProductsQuantity;
    }
}
