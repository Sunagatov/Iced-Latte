package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingSessionItem;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionItemDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = ShoppingSessionItemDtoConverterTest.Config.class)
class ShoppingSessionItemDtoConverterTest {

    @Autowired
    ShoppingSessionItemDtoConverter shoppingSessionItemDtoConverter;

    @Configuration
    public static class Config {

        @Bean
        public ShoppingSessionItemDtoConverter shoppingSessionItemDtoConverter() {
            return Mappers.getMapper(ShoppingSessionItemDtoConverter.class);
        }

        @Bean
        public ProductInfoDtoConverter productInfoDtoConverter() {
            return Mappers.getMapper(ProductInfoDtoConverter.class);
        }
    }

    @Test
    @DisplayName("ToDto should convert ShoppingSessionItem to ShoppingSessionItemDto with complete shopping session item information")
    void shouldConvertShoppingSessionItemToShoppingSessionItemDtoWithCompleteShoppingSessionInformation() {
        ShoppingSessionItem shoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();
        ShoppingSessionItemDto shoppingSessionItemDto = shoppingSessionItemDtoConverter.toDto(shoppingSessionItem);

        assertEquals(shoppingSessionItem.getId(), shoppingSessionItemDto.getId());
        assertEquals(shoppingSessionItem.getProductInfo().getProductId(), shoppingSessionItemDto.getProductInfo().getId());
        assertEquals(shoppingSessionItem.getProductQuantity(), shoppingSessionItemDto.getProductQuantity());
    }
}
