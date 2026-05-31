package com.zufar.icedlatte.cart.exception;

import java.util.UUID;

import lombok.Getter;

@Getter
public final class CartProductSnapshotMissingException extends CartException {

    private final UUID productId;

    public CartProductSnapshotMissingException(final UUID productId) {
        super(String.format("Product snapshot for cart productId = %s is missing.", productId));
        this.productId = productId;
    }
}
