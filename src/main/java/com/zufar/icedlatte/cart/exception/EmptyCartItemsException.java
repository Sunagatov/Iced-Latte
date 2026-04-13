package com.zufar.icedlatte.cart.exception;

public class EmptyCartItemsException extends RuntimeException {
    public EmptyCartItemsException() {
        super("Cart items list must not be empty");
    }
}
