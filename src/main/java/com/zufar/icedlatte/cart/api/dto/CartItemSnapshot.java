package com.zufar.icedlatte.cart.api.dto;

import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

import java.util.UUID;

public record CartItemSnapshot(UUID id,
                               ProductSnapshot product,
                               int productQuantity) {
}
