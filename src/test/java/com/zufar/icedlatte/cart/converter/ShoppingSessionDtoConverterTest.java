package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.api.ItemsTotalPriceCalculator;
import com.zufar.icedlatte.cart.entity.ShoppingSession;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {ShoppingSessionDtoConverterTest.Config.class, ItemsTotalPriceCalculator.class})
class ShoppingSessionDtoConverterTest {

    @Autowired
    ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Configuration
    public static class Config {

        @Bean
        public ShoppingSessionDtoConverter shoppingSessionDtoConverter() {
            return Mappers.getMapper(ShoppingSessionDtoConverter.class);
        }

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
    @DisplayName("ToDto should convert ShoppingSession to ShoppingSessionDto with complete shopping session information")
    void shouldConvertShoppingSessionToShoppingSessionDtoWithCompleteShoppingSessionInformation() {
        ShoppingSession shoppingSession = CartDtoTestStub.createShoppingSession();
        ShoppingSessionDto shoppingSessionDto = shoppingSessionDtoConverter.toDto(shoppingSession);

        assertEquals(shoppingSession.getId(), shoppingSessionDto.getId());
        assertEquals(shoppingSession.getUserId(), shoppingSessionDto.getUserId());
        assertEquals(shoppingSession.getItems().size(), shoppingSessionDto.getItems().size());
        assertEquals(shoppingSession.getItemsQuantity(), shoppingSessionDto.getItemsQuantity());
        assertEquals(shoppingSession.getProductsQuantity(), shoppingSessionDto.getProductsQuantity());
        assertEquals(shoppingSession.getCreatedAt(), shoppingSessionDto.getCreatedAt());
        assertEquals(shoppingSession.getClosedAt(), shoppingSessionDto.getClosedAt());
    }
}
