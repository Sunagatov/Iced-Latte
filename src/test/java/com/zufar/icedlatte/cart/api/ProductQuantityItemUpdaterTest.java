package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.InvalidShoppingCartIdException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQuantityItemUpdaterTest {

    @Mock
    private ShoppingCartItemRepository shoppingCartItemRepository;

    @Mock
    private ShoppingCartProvider shoppingCartProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @InjectMocks
    private ProductQuantityItemUpdater productQuantityItemUpdater;

    @Test
    @DisplayName("Update should return the ShoppingCartDto with new productQuantity when the productQuantityChange is valid")
    void shouldReturnUpdateShoppingCartDtoWithValidProductQuantityChange() throws ShoppingCartNotFoundException, InvalidShoppingCartIdException {
        int productQuantityChange = 5;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID cartId = UUID.randomUUID();
        shoppingCartItem.getShoppingCart().setId(cartId);
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto actualResult = CartDtoTestStub.createShoppingCartDto();
        actualResult.setId(cartId);
        ShoppingCartItem updatedShoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        updatedShoppingCartItem.getShoppingCart().setId(cartId);
        updatedShoppingCartItem.setProductQuantity(shoppingCartItem.getProductQuantity() + productQuantityChange);

        when(shoppingCartItemRepository.findById(shoppingCartItem.getId())).thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(updatedShoppingCartItem);
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(actualResult);

        ShoppingCartDto expectedResult = productQuantityItemUpdater.update(shoppingCartItem.getId(), productQuantityChange);

        assertEquals(expectedResult, actualResult);

        verify(shoppingCartItemRepository, times(1)).findById(shoppingCartItem.getId());
        verify(shoppingCartProvider, times(1)).getByUserId(userDto.getId());
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingCartItemRepository, times(1)).save(shoppingCartItem);
    }

    @Test
    @DisplayName("Update should return the ShoppingCartDto with new productQuantity when the productQuantityChange is less than zero")
    void shouldReturnUpdateShoppingCartDtoWithProductQuantityChangeLessThanZero() throws ShoppingCartNotFoundException, InvalidShoppingCartIdException {
        int productQuantityChange = -2;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID cartId = UUID.randomUUID();
        shoppingCartItem.getShoppingCart().setId(cartId);
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto actualResult = CartDtoTestStub.createShoppingCartDto();
        actualResult.setId(cartId);
        ShoppingCartItem updatedShoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        updatedShoppingCartItem.getShoppingCart().setId(cartId);
        updatedShoppingCartItem.setProductQuantity(shoppingCartItem.getProductQuantity() + productQuantityChange);

        when(shoppingCartItemRepository.findById(shoppingCartItem.getId())).thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(updatedShoppingCartItem);
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(actualResult);

        ShoppingCartDto expectedResult = productQuantityItemUpdater.update(shoppingCartItem.getId(), productQuantityChange);

        assertEquals(expectedResult, actualResult);

        verify(shoppingCartItemRepository, times(1)).findById(shoppingCartItem.getId());
        verify(shoppingCartProvider, times(1)).getByUserId(userDto.getId());
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingCartItemRepository, times(1)).save(shoppingCartItem);
    }

    @Test
    @DisplayName("Update should throw InvalidShoppingCartIdException when shopping cart id is invalid")
    void shouldThrowInvalidShoppingCartIdExceptionWhenShoppingCartIdIsInvalid() throws ShoppingCartNotFoundException, InvalidShoppingCartIdException {
        int productQuantityChange = 5;
        ShoppingCartItem item = CartDtoTestStub.createShoppingCartItem();
        UUID itemId = item.getId();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto shoppingCart = new ShoppingCartDto();
        shoppingCart.setId(UUID.randomUUID());
        shoppingCart.setUserId(userDto.getId());

        when(shoppingCartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(shoppingCartItemRepository.save(item)).thenReturn(item);
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(shoppingCart);

        Assertions.assertThrows(InvalidShoppingCartIdException.class, () -> productQuantityItemUpdater.update(itemId, productQuantityChange));

        verify(shoppingCartItemRepository, times(1)).findById(itemId);
        verify(shoppingCartProvider, times(1)).getByUserId(userDto.getId());
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingCartItemRepository, times(1)).save(item);
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity becomes negative")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityBecomesNegative() {
        int productQuantityChange = -10;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID shoppingCartItemId = shoppingCartItem.getId();

        when(shoppingCartItemRepository.findById(shoppingCartItemId)).thenReturn(Optional.of(shoppingCartItem));

        InvalidItemProductQuantityException exception = assertThrows(
                InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItemId, productQuantityChange)
        );

        assertNotNull(exception);
        verify(shoppingCartItemRepository).findById(shoppingCartItemId);
        verifyNoMoreInteractions(shoppingCartProvider, securityPrincipalProvider);
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity change is zero")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityChangeIsZero() {
        int productQuantityChange = 0;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID shoppingCartItemId = shoppingCartItem.getId();

        when(shoppingCartItemRepository.findById(shoppingCartItemId)).thenReturn(Optional.of(shoppingCartItem));

        InvalidItemProductQuantityException exception = assertThrows(
                InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItemId, productQuantityChange)
        );

        assertNotNull(exception);
        verify(shoppingCartItemRepository).findById(shoppingCartItemId);
        verifyNoMoreInteractions(shoppingCartProvider, securityPrincipalProvider);
    }

    @Test
    @DisplayName("FindById should throw ShoppingCartItemNotFoundException when shopping cart item does not found")
    void shouldThrowShoppingCartItemNotFoundExceptionWhenShoppingCartItemNotFound() {
        UUID nonExistedShoppingCartItemId = UUID.randomUUID();

        when(shoppingCartItemRepository.findById(nonExistedShoppingCartItemId)).thenThrow(new ShoppingCartItemNotFoundException(nonExistedShoppingCartItemId));

        assertThrows(ShoppingCartItemNotFoundException.class, () -> productQuantityItemUpdater.update(nonExistedShoppingCartItemId, 0));

        verify(shoppingCartItemRepository).findById(nonExistedShoppingCartItemId);

        verify(shoppingCartItemRepository, times(1)).findById(nonExistedShoppingCartItemId);
        verify(shoppingCartProvider, times(0)).getByUserId(any(UUID.class));
        verify(securityPrincipalProvider, times(0)).get();
        verify(shoppingCartItemRepository, times(0)).save(any(ShoppingCartItem.class));
    }
}