package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoppingCartDtoConverterTest {

    private static ShoppingCartDtoConverter shoppingCartDtoConverter;

    @BeforeAll
    public static void setUp() {
        ProductInfoDtoConverter productInfoDtoConverter = new ProductInfoDtoConverter();
        ShoppingCartItemDtoConverter shoppingCartItemDtoConverter = new ShoppingCartItemDtoConverter(productInfoDtoConverter);
        shoppingCartDtoConverter = new ShoppingCartDtoConverter(shoppingCartItemDtoConverter);
    }

    @Test
    @DisplayName("ToDto should convert ShoppingCart to ShoppingCartDto with complete shopping cart information")
    void shouldConvertShoppingCartToShoppingCartDtoWithCompleteShoppingCartInformation() {
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();

        ShoppingCartDto shoppingCartDto = shoppingCartDtoConverter.toDto(shoppingCart);

        assertEquals(shoppingCart.getId(), shoppingCartDto.getId());
        assertEquals(shoppingCart.getUserId(), shoppingCartDto.getUserId());
        assertEquals(shoppingCart.getItems().size(), shoppingCartDto.getItems().size());
        assertEquals(shoppingCart.getItemsQuantity(), shoppingCartDto.getItemsQuantity());
        assertEquals(BigDecimal.valueOf(6.6), shoppingCartDto.getItemsTotalPrice());
        assertEquals(shoppingCart.getProductsQuantity(), shoppingCartDto.getProductsQuantity());
        assertEquals(shoppingCart.getCreatedAt(), shoppingCartDto.getCreatedAt());
        assertEquals(shoppingCart.getClosedAt(), shoppingCartDto.getClosedAt());
    }
}
