package com.zufar.icedlatte.cart.exception;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;

@Getter
public final class CartProductNotFoundException extends CartException {

    private final List<UUID> productIds;

    public CartProductNotFoundException(final List<UUID> productIds) {
        super(String.format(
                "Products with productIds = %s are not found.",
                productIds.stream().map(UUID::toString).collect(Collectors.joining(", "))));
        this.productIds = List.copyOf(productIds);
    }
}
