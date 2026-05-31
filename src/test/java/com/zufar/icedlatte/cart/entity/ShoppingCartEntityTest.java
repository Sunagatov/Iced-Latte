package com.zufar.icedlatte.cart.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shopping cart entity tests")
class ShoppingCartEntityTest {

    @Test
    @DisplayName("shopping cart hash code stays stable when id is assigned")
    void shoppingCartHashCodeStaysStableWhenIdIsAssigned() {
        ShoppingCart cart = new ShoppingCart();
        int hashBeforeId = cart.hashCode();

        cart.setId(UUID.randomUUID());

        assertThat(cart.hashCode()).isEqualTo(hashBeforeId);
    }

    @Test
    @DisplayName("shopping cart item remains findable in hash set after id is assigned")
    void shoppingCartItemRemainsFindableInHashSetAfterIdIsAssigned() {
        ShoppingCartItem item = ShoppingCartItem.builder()
                .productId(UUID.randomUUID())
                .productQuantity(1)
                .build();
        HashSet<ShoppingCartItem> items = new HashSet<>();
        items.add(item);

        item.setId(UUID.randomUUID());

        assertThat(items).contains(item);
    }

    @Test
    @DisplayName("transient cart items are not equal unless they are the same instance")
    void transientCartItemsAreNotEqualUnlessSameInstance() {
        UUID productId = UUID.randomUUID();
        ShoppingCartItem first = ShoppingCartItem.builder()
                .productId(productId)
                .productQuantity(1)
                .build();
        ShoppingCartItem second = ShoppingCartItem.builder()
                .productId(productId)
                .productQuantity(1)
                .build();

        assertThat(first).isNotEqualTo(second);
    }
}
