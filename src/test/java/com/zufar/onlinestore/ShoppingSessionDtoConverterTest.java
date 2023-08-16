package com.zufar.onlinestore;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ShoppingSessionDtoConverterTest {

    @Autowired
    private ShoppingSessionDtoConverter converter;
    private ShoppingSession shoppingSession;

    @BeforeEach
    void setUp() {
        shoppingSession = Instancio.create(ShoppingSession.class);
    }

    @Test
    void shouldCheckShoppingSessionDoesNotEqualToNull() {
        assertNotNull(shoppingSession);
    }

    @Test
    void shouldCheckShoppingSessionDtoDoesNotEqualToNull() {
        ShoppingSessionDto dto = converter.toDto(shoppingSession);

        assertNotNull(dto);
    }

    @Test
    void shouldMapShoppingSessionToDto() {
        ShoppingSessionDto dto = converter.toDto(shoppingSession);

        assertAll(
                () -> assertEquals(dto.id(), shoppingSession.getId()),
                () -> assertEquals(dto.userId(), shoppingSession.getUserId()),
                () -> assertEquals(dto.createdAt(), shoppingSession.getCreatedAt()),
                () -> assertEquals(dto.productsQuantity(), shoppingSession.getProductsQuantity())
        );
    }
}