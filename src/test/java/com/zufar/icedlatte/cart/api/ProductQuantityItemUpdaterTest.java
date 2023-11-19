package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingSessionItem;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.icedlatte.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingSessionItemRepository;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQuantityItemUpdaterTest {

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
    void shouldReturnUpdateShoppingSessionDtoWithValidProductQuantityChange() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
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
    void shouldReturnUpdateShoppingSessionDtoWithProductQuantityChangeLessThanZero() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
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
    void shouldThrowInvalidShoppingSessionIdExceptionWhenShoppingSessionIdIsInvalid() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
        int productQuantityChange = 5;
        ShoppingSessionItem item = CartDtoTestStub.createShoppingSessionItem();
        UUID itemId = item.getId();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingSessionDto shoppingSession = new ShoppingSessionDto();
        shoppingSession.setId(UUID.randomUUID());
        shoppingSession.setUserId(userDto.getId());

        when(shoppingSessionItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(shoppingSessionItemRepository.save(item)).thenReturn(item);
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(shoppingSession);

        assertThrows(InvalidShoppingSessionIdException.class, () -> productQuantityItemUpdater.update(itemId, productQuantityChange));

        verify(shoppingSessionItemRepository, times(1)).findById(itemId);
        verify(shoppingSessionProvider, times(1)).getByUserId(userDto.getId());
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingSessionItemRepository, times(1)).save(item);
    }

    @Test
    @DisplayName("FindById should throw InvalidItemProductQuantityException when attempted to set negative products quantity for item")
    void shouldThrowInvalidItemProductQuantityExceptionWhenProductQuantitySetNegative() {
        int productQuantityChange = -10;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();
        UUID shoppingSessionItemId = shoppingSessionItem.getId();

        when(shoppingSessionItemRepository.findById(shoppingSessionItemId)).thenThrow(new InvalidItemProductQuantityException(productQuantityChange));

        assertThrows(InvalidItemProductQuantityException.class, () -> productQuantityItemUpdater.update(shoppingSessionItemId, productQuantityChange));

        verify(shoppingSessionItemRepository, times(1)).findById(shoppingSessionItemId);
        verify(shoppingSessionProvider, times(0)).getByUserId(any(UUID.class));
        verify(securityPrincipalProvider, times(0)).get();
        verify(shoppingSessionItemRepository, times(0)).save(any(ShoppingSessionItem.class));
    }

    @Test
    @DisplayName("FindById should throw InvalidItemProductQuantityException when attempted to sent zero products quantity change")
    void shouldThrowInvalidItemProductQuantityExceptionWhenProductQuantityChangeIsZero() {
        int productQuantityChange = 0;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestStub.createShoppingSessionItem();
        UUID shoppingSessionItemId = shoppingSessionItem.getId();

        when(shoppingSessionItemRepository.findById(shoppingSessionItemId)).thenThrow(new InvalidItemProductQuantityException(productQuantityChange));

        assertThrows(InvalidItemProductQuantityException.class, () -> productQuantityItemUpdater.update(shoppingSessionItemId, productQuantityChange));

        verify(shoppingSessionItemRepository, times(1)).findById(shoppingSessionItemId);
        verify(shoppingSessionProvider, times(0)).getByUserId(any(UUID.class));
        verify(securityPrincipalProvider, times(0)).get();
        verify(shoppingSessionItemRepository, times(0)).save(any(ShoppingSessionItem.class));
    }

    @Test
    @DisplayName("FindById should throw ShoppingSessionItemNotFoundException when shopping session item does not found")
    void shouldThrowShoppingSessionItemNotFoundExceptionWhenShoppingSessionItemNotFound() {
        UUID nonExistedShoppingSessionItemId = UUID.randomUUID();

        when(shoppingSessionItemRepository.findById(nonExistedShoppingSessionItemId)).thenThrow(new ShoppingSessionItemNotFoundException(nonExistedShoppingSessionItemId));

        assertThrows(ShoppingSessionItemNotFoundException.class, () -> productQuantityItemUpdater.update(nonExistedShoppingSessionItemId, 0));

        verify(shoppingSessionItemRepository).findById(nonExistedShoppingSessionItemId);

        verify(shoppingSessionItemRepository, times(1)).findById(nonExistedShoppingSessionItemId);
        verify(shoppingSessionProvider, times(0)).getByUserId(any(UUID.class));
        verify(securityPrincipalProvider, times(0)).get();
        verify(shoppingSessionItemRepository, times(0)).save(any(ShoppingSessionItem.class));
    }
}