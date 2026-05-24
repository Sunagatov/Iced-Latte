package com.zufar.icedlatte.cart.api;

import java.util.Set;
import java.util.UUID;

import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;

/**
 * Narrow contract exposed to payment and order modules. Provides checkout-related cart operations and reorder item
 * addition.
 */
public interface CartCheckoutApi {

    CartSnapshot getByUserIdOrThrow(UUID userId);

    void deleteCartForUser(UUID userId);

    CartSnapshot addItems(UUID userId, Set<AddCartItemRequest> items);
}
