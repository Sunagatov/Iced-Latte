package com.zufar.icedlatte.cart.api.dto;

import java.util.Objects;
import java.util.UUID;

public record AddCartItemRequest(UUID productId, int productQuantity) {

    public AddCartItemRequest {
        Objects.requireNonNull(productId, "productId must not be null");
    }
}
