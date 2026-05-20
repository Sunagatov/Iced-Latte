package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingCartDtoConverterTest {

    private final ShoppingCartDtoConverter converter = new ShoppingCartDtoConverter();

    @Test
    @DisplayName("Should convert ShoppingCart to ShoppingCartDto with complete information")
    void shouldConvertShoppingCartToShoppingCartDtoWithCompleteInformation() {
        ShoppingCart cart = CartDtoTestStub.createShoppingCart();
        Map<UUID, ProductInfoDto> productsById = CartDtoTestStub.createProductsById();

        ShoppingCartDto result = converter.toDto(cart, productsById);

        assertNotNull(result);
        assertEquals(cart.getId(), result.getId());
        assertEquals(cart.getUserId(), result.getUserId());
        assertEquals(3, result.getItems().size());
        assertEquals(3, result.getItemsQuantity());
        assertEquals(6, result.getProductsQuantity());
        // 1*1.1 + 2*2.2 + 3*3.3 = 1.1 + 4.4 + 9.9 = 15.4
        assertEquals(0, new BigDecimal("15.4").compareTo(result.getItemsTotalPrice()));
    }

    @Test
    @DisplayName("Should convert empty shopping cart correctly")
    void shouldConvertEmptyShoppingCartCorrectly() {
        ShoppingCart emptyCart = CartDtoTestStub.createEmptyShoppingCart();
        Map<UUID, ProductInfoDto> productsById = Map.of();

        ShoppingCartDto result = converter.toDto(emptyCart, productsById);

        assertNotNull(result);
        assertEquals(emptyCart.getId(), result.getId());
        assertEquals(emptyCart.getUserId(), result.getUserId());
        assertEquals(0, result.getItems().size());
        assertEquals(0, result.getItemsQuantity());
        assertEquals(0, result.getProductsQuantity());
        assertEquals(BigDecimal.ZERO, result.getItemsTotalPrice());
    }
}
