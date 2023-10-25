package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.cart.stub.CartDtoTestUtil;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShoppingSessionProviderTest {

    @InjectMocks
    ShoppingSessionProvider shoppingSessionProvider;

    @Mock
    ShoppingSessionRepository shoppingSessionRepository;

    @Mock
    ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Test
    @DisplayName("getByUserId should return the correct ShoppingSessionDto when the user exists")
    public void getByUserId_ShouldReturnCorrectShoppingSessionDtoWhenUserExists() throws ShoppingSessionNotFoundException {
        UUID userId = UUID.fromString("2eebb17c-5a55-43dd-add7-c15d49521f14");
        ShoppingSession expectedShoppingSession = CartDtoTestUtil.createShoppingSession();

        when(shoppingSessionRepository.findShoppingSessionByUserId(userId)).thenReturn(expectedShoppingSession);

        ShoppingSessionDto actualShoppingSessionDto = shoppingSessionProvider.getByUserId(userId);

        assertEquals(shoppingSessionDtoConverter.toDto(expectedShoppingSession), actualShoppingSessionDto);
        verify(shoppingSessionRepository).findShoppingSessionByUserId(userId);
    }

    @Test
    @DisplayName("getByUserId should throw ShoppingSessionNotFoundException when the shopping session does not exist")
    public void getByUserId_ShouldThrowShoppingSessionNotFoundExceptionWhenShoppingSessionDoesNotExist() {
        UUID nonExistentUserId = UUID.randomUUID();

        when(shoppingSessionRepository.findShoppingSessionByUserId(nonExistentUserId)).thenThrow(ShoppingSessionNotFoundException.class);

        assertThrows(ShoppingSessionNotFoundException.class, () -> shoppingSessionProvider.getByUserId(nonExistentUserId));
        verify(shoppingSessionRepository).findShoppingSessionByUserId(nonExistentUserId);
    }
}
