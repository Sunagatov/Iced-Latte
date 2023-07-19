package com.zufar.onlinestore.cart.dto;

import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.product.dto.ProductInfoFullDto;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShoppingSessionItemDto(

        @NotNull(message = "Id is mandatory")
        UUID id,

        @NotNull(message = "Cart is mandatory")
        ShoppingSession cart,

        @NotNull(message = "ProductInfo is mandatory")
        ProductInfoFullDto productInfo,

        @NotNull(message = "ProductsQuantity is mandatory")
        Integer productsQuantity
) {
}
