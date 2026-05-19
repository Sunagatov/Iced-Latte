package com.zufar.icedlatte.cart.exception;

/**
 * Sealed base for all cart-related exceptions.
 * Enables exhaustive pattern matching in switch expressions (Java 25).
 */
public abstract sealed class CartException extends RuntimeException
        permits ShoppingCartNotFoundException, ShoppingCartItemNotFoundException,
                InvalidItemProductQuantityException {

    protected CartException(String message) {
        super(message);
    }
}
