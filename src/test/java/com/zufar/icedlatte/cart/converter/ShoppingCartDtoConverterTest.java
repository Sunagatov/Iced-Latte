package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.api.ItemsTotalPriceCalculator;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {ShoppingCartDtoConverterTest.Config.class, ItemsTotalPriceCalculator.class})
class ShoppingCartDtoConverterTest {

    @Autowired
    ShoppingCartDtoConverter shoppingCartDtoConverter;

    @Configuration
    public static class Config {

        @Bean
        public ShoppingCartDtoConverter shoppingCartDtoConverter() {
            return Mappers.getMapper(ShoppingCartDtoConverter.class);
        }

        @Bean
        public ShoppingCartItemDtoConverter shoppingCartItemDtoConverter() {
            return Mappers.getMapper(ShoppingCartItemDtoConverter.class);
        }

        @Bean
        public ProductInfoDtoConverter productInfoDtoConverter() {
            return Mappers.getMapper(ProductInfoDtoConverter.class);
        }
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
        assertEquals(shoppingCart.getProductsQuantity(), shoppingCartDto.getProductsQuantity());
        assertEquals(shoppingCart.getCreatedAt(), shoppingCartDto.getCreatedAt());
        assertEquals(shoppingCart.getClosedAt(), shoppingCartDto.getClosedAt());
    }
}
