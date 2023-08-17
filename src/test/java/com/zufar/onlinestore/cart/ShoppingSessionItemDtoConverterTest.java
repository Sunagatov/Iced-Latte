package com.zufar.onlinestore.cart;

import com.zufar.onlinestore.cart.converter.ShoppingSessionItemDtoConverter;
import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ShoppingSessionItemDtoConverterTest {

    @Autowired
    private ShoppingSessionItemDtoConverter converter;
    private ShoppingSessionItem shoppingSessionItem;

    @BeforeEach
    void setUp() {
        shoppingSessionItem = Instancio.create(ShoppingSessionItem.class);
    }

    @Test
    void shouldCheckShoppingSessionItemDoesNotEqualToNull() {
        assertNotNull(shoppingSessionItem);
    }

    @Test
    void shouldCheckShoppingSessionItemDtoDoesNotEqualToNull() {
        ShoppingSessionItemDto dto = converter.toDto(shoppingSessionItem);

        assertNotNull(dto);
    }

    @Test
    void shouldMapShoppingSessionItemToDto() {
        ShoppingSessionItemDto dto = converter.toDto(shoppingSessionItem);

        assertAll(
                () -> assertEquals(dto.id(), shoppingSessionItem.getId()),
                () -> assertEquals(dto.productsQuantity(), shoppingSessionItem.getProductsQuantity()),
                () -> assertEquals(dto.productInfo().id(), shoppingSessionItem.getProductInfo().getProductId()),
                () -> assertEquals(dto.productInfo().name(), shoppingSessionItem.getProductInfo().getName())
        );
    }
}