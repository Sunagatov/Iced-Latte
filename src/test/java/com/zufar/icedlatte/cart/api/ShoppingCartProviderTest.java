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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
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
    ShoppingCartDtoConverter shoppingCartDtoConverter;

    @Test
    @DisplayName("GetByUserId should return the correct ShoppingCartDto when the user exists")
    void shouldReturnCorrectShoppingCartDtoWhenUserExists() throws ShoppingCartNotFoundException {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingCart expectedShoppingCart = CartDtoTestStub.createShoppingCart();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(expectedShoppingCart);

        ShoppingCartDto actualShoppingCartDto = shoppingCartProvider.getByUserId(userId);

        assertEquals(shoppingCartDtoConverter.toDto(expectedShoppingCart), actualShoppingCartDto);
        verify(shoppingCartRepository).findShoppingCartByUserId(userId);
        verify(shoppingCartDtoConverter, times(2)).toDto(expectedShoppingCart);
    }

    @Test
    @DisplayName("GetByUserId should throw ShoppingCartNotFoundException when the shopping cart does not exist")
    void shouldThrowShoppingCartNotFoundExceptionWhenShoppingCartDoesNotExist() {
        UUID nonExistentUserId = UUID.randomUUID();

        when(shoppingCartRepository.findShoppingCartByUserId(nonExistentUserId)).thenThrow(ShoppingCartNotFoundException.class);

        assertThrows(ShoppingCartNotFoundException.class, () -> shoppingCartProvider.getByUserId(nonExistentUserId));
        verify(shoppingCartRepository, times(1)).findShoppingCartByUserId(nonExistentUserId);
        verify(shoppingCartDtoConverter, never()).toDto(new ShoppingCart());
    }
}
