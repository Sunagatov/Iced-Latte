package com.zufar.onlinestore;

import com.zufar.onlinestore.cart.converter.ShoppingSessionItemDtoConverter;
import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ShoppingSessionItemDtoConverterTest {

    @Autowired
    private ShoppingSessionItemDtoConverter converter
            = Mappers.getMapper(ShoppingSessionItemDtoConverter.class);

    @Test
    public void mapping_ShoppingSessionItem_to_ShoppingSessionItemDto() {
        ShoppingSessionItem shoppingSessionItem = Instancio.create(ShoppingSessionItem.class);

        ShoppingSessionItemDto dto = converter.toDto(shoppingSessionItem);

        assertAll(
                () -> assertNotNull(dto),
                () -> assertNotNull(shoppingSessionItem),
                () -> assertEquals(dto.id(), shoppingSessionItem.getId()),
                () -> assertEquals(dto.productsQuantity(), shoppingSessionItem.getProductsQuantity()),
                () -> assertEquals(dto.productInfo().id(), shoppingSessionItem.getProductInfo().getProductId()),
                () -> assertEquals(dto.productInfo().name(), shoppingSessionItem.getProductInfo().getName())
        );
    }
}