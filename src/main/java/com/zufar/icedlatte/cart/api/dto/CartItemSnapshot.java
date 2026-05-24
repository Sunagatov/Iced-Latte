package com.zufar.icedlatte.cart.api.dto;

import java.util.UUID;

import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

public record CartItemSnapshot(UUID id, ProductSnapshot product, int productQuantity) {}
