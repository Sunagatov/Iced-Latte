package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;

import java.util.Set;
import java.util.UUID;

/**
 * Narrow contract exposed to payment and order modules.
 * Provides checkout-related cart operations and reorder item addition.
 */
public interface CartCheckoutApi {

    ShoppingCartDto getByUserIdOrThrow(UUID userId);

    void deleteCartForUser(UUID userId);

    ShoppingCartDto addItems(UUID userId, Set<NewShoppingCartItemDto> items);
}
