package com.zufar.icedlatte.payment.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ShoppingCartAlreadyPaidException extends RuntimeException {

    private final UUID shoppingCartId;

    public ShoppingCartAlreadyPaidException(UUID shoppingCartId) {
        super(String.format("Shopping cart with shoppingCartId = %s is already paid", shoppingCartId));
        this.shoppingCartId = shoppingCartId;
    }
}
