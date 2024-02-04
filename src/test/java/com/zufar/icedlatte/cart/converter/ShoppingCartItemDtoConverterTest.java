package com.zufar.icedlatte.cart.converter;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ShoppingCartItemDtoConverterTest {

    @InjectMocks
    private ShoppingCartItemDtoConverter shoppingCartItemDtoConverter;

    @Mock
    private ProductInfoDtoConverter productInfoDtoConverter;

    @Test
    @DisplayName("ToDto should convert ShoppingCartItem to ShoppingCartItemDto with complete shopping cart item information")
    void shouldConvertShoppingCartItemToShoppingCartItemDtoWithCompleteShoppingCartInformation() {
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        ShoppingCartItemDto shoppingCartItemDto = shoppingCartItemDtoConverter.toDto(shoppingCartItem);

        assertEquals(shoppingCartItem.getId(), shoppingCartItemDto.getId());
        assertEquals(shoppingCartItem.getProductQuantity(), shoppingCartItemDto.getProductQuantity());
    }
}
