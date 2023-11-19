package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingSession;
import com.zufar.icedlatte.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingSessionRepository;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
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
class ShoppingSessionProviderTest {

    @InjectMocks
    ShoppingSessionProvider shoppingSessionProvider;

    @Mock
    ShoppingSessionRepository shoppingSessionRepository;

    @Mock
    ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Test
    @DisplayName("GetByUserId should return the correct ShoppingSessionDto when the user exists")
    void shouldReturnCorrectShoppingSessionDtoWhenUserExists() throws ShoppingSessionNotFoundException {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingSession expectedShoppingSession = CartDtoTestStub.createShoppingSession();

        when(shoppingSessionRepository.findShoppingSessionByUserId(userId)).thenReturn(expectedShoppingSession);

        ShoppingSessionDto actualShoppingSessionDto = shoppingSessionProvider.getByUserId(userId);

        assertEquals(shoppingSessionDtoConverter.toDto(expectedShoppingSession), actualShoppingSessionDto);
        verify(shoppingSessionRepository).findShoppingSessionByUserId(userId);
        verify(shoppingSessionDtoConverter, times(2)).toDto(expectedShoppingSession);
    }

    @Test
    @DisplayName("GetByUserId should throw ShoppingSessionNotFoundException when the shopping session does not exist")
    void shouldThrowShoppingSessionNotFoundExceptionWhenShoppingSessionDoesNotExist() {
        UUID nonExistentUserId = UUID.randomUUID();

        when(shoppingSessionRepository.findShoppingSessionByUserId(nonExistentUserId)).thenThrow(ShoppingSessionNotFoundException.class);

        assertThrows(ShoppingSessionNotFoundException.class, () -> shoppingSessionProvider.getByUserId(nonExistentUserId));
        verify(shoppingSessionRepository, times(1)).findShoppingSessionByUserId(nonExistentUserId);
        verify(shoppingSessionDtoConverter, never()).toDto(new ShoppingSession());
    }
}
