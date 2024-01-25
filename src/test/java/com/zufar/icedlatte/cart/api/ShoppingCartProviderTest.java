package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

import static com.zufar.icedlatte.cart.api.ShoppingCartCreator.DEFAULT_ITEMS_QUANTITY;
import static com.zufar.icedlatte.cart.api.ShoppingCartCreator.DEFAULT_PRODUCTS_QUANTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartProviderTest {

    @InjectMocks
    ShoppingCartProvider shoppingCartProvider;

    @Mock
    ShoppingCartRepository shoppingCartRepository;

    @Mock
    ShoppingCartCreator shoppingCartCreator;

    @Mock
    ShoppingCartDtoConverter shoppingCartDtoConverter;

    @Test
    @DisplayName("GetByUserId should return the correct ShoppingCartDto when the user exists")
    void shouldReturnCorrectShoppingCartDtoWhenUserExists() throws ShoppingCartNotFoundException {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingCart expectedShoppingCart = CartDtoTestStub.createShoppingCart();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(expectedShoppingCart);

        ShoppingCartDto actualShoppingCartDto = shoppingCartProvider.getByUserId(userId);

        assertEquals(shoppingCartDtoConverter.toDto(expectedShoppingCart), actualShoppingCartDto);
        verify(shoppingCartRepository, times(1)).findShoppingCartByUserId(userId);
        verify(shoppingCartCreator, times(0)).createNewShoppingCart(userId);
        verify(shoppingCartDtoConverter, times(2)).toDto(expectedShoppingCart);
    }

    @Test
    @DisplayName("GetByUserId should create a new ShoppingCart when the shopping cart does not exist")
    void shouldCreateNewShoppingCartWhenShoppingCartDoesNotExist() {
        UUID userId = UUID.randomUUID();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .itemsQuantity(DEFAULT_ITEMS_QUANTITY)
                .productsQuantity(DEFAULT_PRODUCTS_QUANTITY)
                .items(new HashSet<>())
                .createdAt(OffsetDateTime.now())
                .build();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(null);
        when(shoppingCartCreator.createNewShoppingCart(userId)).thenReturn(shoppingCart);

        ShoppingCartDto actualShoppingCartDto = shoppingCartProvider.getByUserId(userId);

        assertEquals(shoppingCartDtoConverter.toDto(shoppingCart), actualShoppingCartDto);
        verify(shoppingCartRepository, times(1)).findShoppingCartByUserId(userId);
        verify(shoppingCartCreator, times(1)).createNewShoppingCart(userId);
        verify(shoppingCartDtoConverter, times(2)).toDto(shoppingCart);
    }
}
