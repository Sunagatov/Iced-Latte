package com.zufar.onlinestore.product.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ProductNotFoundException extends RuntimeException {

    private final UUID productId;

    public ProductNotFoundException(final UUID productId) {
        super(String.format("The product with productId = %s  is not found.",productId));
        this.productId = productId;
    }
}
