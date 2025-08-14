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

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;

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
    @DisplayName("Should convert ShoppingCart to ShoppingCartDto with complete information")
    void shouldConvertShoppingCartToShoppingCartDtoWithCompleteInformation() {
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();
        
        ShoppingCartDto result = shoppingCartDtoConverter.toDto(shoppingCart);

        assertNotNull(result);
        assertEquals(shoppingCart.getId(), result.getId());
        assertEquals(shoppingCart.getUserId(), result.getUserId());
        assertEquals(shoppingCart.getItems().size(), result.getItems().size());
        assertEquals(shoppingCart.getItemsQuantity(), result.getItemsQuantity());
        assertEquals(shoppingCart.getProductsQuantity(), result.getProductsQuantity());
        assertEquals(shoppingCart.getCreatedAt(), result.getCreatedAt());
        assertEquals(shoppingCart.getClosedAt(), result.getClosedAt());
        
        assertThat(result.getItemsTotalPrice(), greaterThanOrEqualTo(BigDecimal.ZERO));
        assertThat(result.getItems(), hasSize(shoppingCart.getItems().size()));
    }
    
    @Test
    @DisplayName("Should handle null shopping cart gracefully")
    void shouldHandleNullShoppingCartGracefully() {
        ShoppingCartDto result = shoppingCartDtoConverter.toDto(null);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Should convert empty shopping cart correctly")
    void shouldConvertEmptyShoppingCartCorrectly() {
        ShoppingCart emptyCart = CartDtoTestStub.createEmptyShoppingCart();
        
        ShoppingCartDto result = shoppingCartDtoConverter.toDto(emptyCart);
        
        assertNotNull(result);
        assertEquals(emptyCart.getId(), result.getId());
        assertEquals(emptyCart.getUserId(), result.getUserId());
        assertThat(result.getItems(), hasSize(0));
        assertEquals(0, result.getItemsQuantity());
        assertEquals(0, result.getProductsQuantity());
        assertEquals(BigDecimal.ZERO, result.getItemsTotalPrice());
    }
}
