package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.InvalidItemProductQuantityException;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.stub.CartDtoTestStub;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductQuantityItemUpdaterTest {

    @Mock
    private ShoppingSessionItemRepository shoppingSessionItemRepository;

    @Mock
    private ShoppingSessionProvider shoppingSessionProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @InjectMocks
    private ProductQuantityItemUpdater productQuantityItemUpdater;

    @Test
    @DisplayName("Update should return the ShoppingSessionDto with new productQuantity when the productQuantityChange is valid")
    public void shouldReturnUpdateShoppingSessionDtoWithValidProductQuantityChange() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
        int productQuantityChange = 5;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingSessionDto actualResult = CartDtoTestStub.createShoppingSessionDto();
        ShoppingSessionItem updatedShoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();
        updatedShoppingSessionItem.setProductQuantity(shoppingSessionItem.getProductQuantity() + productQuantityChange);

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenReturn(Optional.of(shoppingSessionItem));
        when(shoppingSessionItemRepository.save(shoppingSessionItem)).thenReturn(updatedShoppingSessionItem);
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(actualResult);

        ShoppingSessionDto expectedResult = productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);

        assertEquals(expectedResult, actualResult);

        verify(shoppingSessionItemRepository, times(1)).findById(shoppingSessionItem.getId());
        verify(shoppingSessionProvider, times(1)).getByUserId(userDto.getId());
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingSessionItemRepository, times(1)).save(shoppingSessionItem);
    }

    @Test
    @DisplayName("Update should return the ShoppingSessionDto with new productQuantity when the productQuantityChange is less than zero")
    public void shouldReturnUpdateShoppingSessionDtoWithProductQuantityChangeLessThanZero() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
        int productQuantityChange = -5;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingSessionDto actualResult = CartDtoTestStub.createShoppingSessionDto();
        ShoppingSessionItem updatedShoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();
        updatedShoppingSessionItem.setProductQuantity(shoppingSessionItem.getProductQuantity() + productQuantityChange);

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenReturn(Optional.of(shoppingSessionItem));
        when(shoppingSessionItemRepository.save(shoppingSessionItem)).thenReturn(updatedShoppingSessionItem);
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(actualResult);

        ShoppingSessionDto expectedResult = productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);

        assertEquals(expectedResult, actualResult);

        verify(shoppingSessionItemRepository, times(1)).findById(shoppingSessionItem.getId());
        verify(shoppingSessionProvider, times(1)).getByUserId(userDto.getId());
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingSessionItemRepository, times(1)).save(shoppingSessionItem);
    }

    @Test
    @DisplayName("Update should throw InvalidShoppingSessionIdException when shopping session id is invalid")
    public void shouldThrowInvalidShoppingSessionIdExceptionWhenShoppingSessionIdIsInvalid() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
        int productQuantityChange = 5;
        ShoppingSessionItem item = CartDtoTestStub.createShoppingSessionItem();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingSessionDto shoppingSession = new ShoppingSessionDto();
        shoppingSession.setId(UUID.randomUUID());
        shoppingSession.setUserId(userDto.getId());

        when(shoppingSessionItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(shoppingSessionItemRepository.save(item)).thenReturn(item);
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(shoppingSession);

        assertThrows(InvalidShoppingSessionIdException.class, () -> {
            productQuantityItemUpdater.update(item.getId(), productQuantityChange);
        });

        verify(shoppingSessionItemRepository, times(1)).findById(item.getId());
        verify(shoppingSessionProvider, times(1)).getByUserId(userDto.getId());
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingSessionItemRepository, times(1)).save(item);
    }

    @Test
    @DisplayName("FindById should throw InvalidItemProductQuantityException when attempted to set negative products quantity for item")
    public void shouldThrowInvalidItemProductQuantityExceptionWhenProductQuantitySetNegative() {
        int productQuantityChange = -10;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenThrow(new InvalidItemProductQuantityException(productQuantityChange));

        assertThrows(InvalidItemProductQuantityException.class, () -> {
            productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);
        });
        verify(shoppingSessionItemRepository).findById(shoppingSessionItem.getId());
    }

    @Test
    @DisplayName("FindById should throw InvalidItemProductQuantityException when attempted to sent zero products quantity change")
    public void shouldThrowInvalidItemProductQuantityExceptionWhenProductQuantityChangeIsZero() {
        int productQuantityChange = 0;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenThrow(new InvalidItemProductQuantityException(productQuantityChange));

        assertThrows(InvalidItemProductQuantityException.class, () -> {
            productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);
        });
        verify(shoppingSessionItemRepository).findById(shoppingSessionItem.getId());
    }

    @Test
    @DisplayName("FindById should throw ShoppingSessionItemNotFoundException when shopping session item does not found")
    public void shouldThrowShoppingSessionItemNotFoundExceptionWhenShoppingSessionItemNotFound() {
        UUID nonExistedShoppingSessionItemId = UUID.randomUUID();

        when(shoppingSessionItemRepository.findById(nonExistedShoppingSessionItemId)).thenThrow(new ShoppingSessionItemNotFoundException(nonExistedShoppingSessionItemId));

        assertThrows(ShoppingSessionItemNotFoundException.class, () -> {
            productQuantityItemUpdater.update(nonExistedShoppingSessionItemId, 0);
        });
        verify(shoppingSessionItemRepository).findById(nonExistedShoppingSessionItemId);
    }
}