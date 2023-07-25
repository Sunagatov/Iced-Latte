package com.zufar.onlinestore.product.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ProductNotFoundException extends RuntimeException {

    private final Integer productId;

    public ProductNotFoundException(final Integer productId) {
        super(String.format("The product with id = %s is not found.", productId));
        this.productId = productId;
    }
}
