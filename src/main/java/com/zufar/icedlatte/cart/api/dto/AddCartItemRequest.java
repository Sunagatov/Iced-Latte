package com.zufar.icedlatte.cart.api.dto;

import java.util.UUID;

public record AddCartItemRequest(UUID productId,
                                 int productQuantity) {
}
