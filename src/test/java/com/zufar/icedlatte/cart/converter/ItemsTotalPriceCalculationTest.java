package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShoppingCartDtoConverter.toItemsTotalPrice unit tests")
class ItemsTotalPriceCalculationTest {

    private final ShoppingCartDtoConverter converter = _ -> null;

    @Test
    @DisplayName("returns total price of all shopping cart items")
    void returnsTotalPriceOfAllShoppingCartItems() {
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();

        BigDecimal result = converter.toItemsTotalPrice(shoppingCart.getItems());

        assertThat(result).isEqualByComparingTo(new BigDecimal("15.4"));
    }

    @Test
    @DisplayName("returns zero for empty item set")
    void returnsZeroForEmptyItemSet() {
        BigDecimal result = converter.toItemsTotalPrice(Set.of());

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("returns zero for null item set")
    void returnsZeroForNullItemSet() {
        BigDecimal result = converter.toItemsTotalPrice(null);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
