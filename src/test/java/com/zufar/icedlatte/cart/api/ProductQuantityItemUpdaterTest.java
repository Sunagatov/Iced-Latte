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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @DisplayName("Update should return fresh ShoppingCartDto after save when productQuantityChange is valid")
    void shouldReturnUpdateShoppingCartDtoWithValidProductQuantityChange() throws ShoppingCartNotFoundException, InvalidShoppingCartIdException {
        int productQuantityChange = 5;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID cartId = UUID.randomUUID();
        shoppingCartItem.getShoppingCart().setId(cartId);
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto cartDto = CartDtoTestStub.createShoppingCartDto();
        cartDto.setId(cartId);

        when(securityPrincipalProvider.getUserId()).thenReturn(userDto.getId());
        when(shoppingCartItemRepository.findById(shoppingCartItem.getId())).thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(cartDto);
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(shoppingCartItem);

        ShoppingCartDto result = productQuantityItemUpdater.update(shoppingCartItem.getId(), productQuantityChange);

        assertEquals(cartDto, result);
        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartItemRepository).findById(shoppingCartItem.getId());
        verify(shoppingCartProvider, times(2)).getByUserId(userDto.getId());
        verify(shoppingCartItemRepository).save(shoppingCartItem);
    }

    @Test
    @DisplayName("Update should return fresh ShoppingCartDto after save when productQuantityChange is negative but result >= 1")
    void shouldReturnUpdateShoppingCartDtoWithProductQuantityChangeLessThanZero() throws ShoppingCartNotFoundException, InvalidShoppingCartIdException {
        int productQuantityChange = -2; // item has quantity 5, result = 3
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID cartId = UUID.randomUUID();
        shoppingCartItem.getShoppingCart().setId(cartId);
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto cartDto = CartDtoTestStub.createShoppingCartDto();
        cartDto.setId(cartId);

        when(securityPrincipalProvider.getUserId()).thenReturn(userDto.getId());
        when(shoppingCartItemRepository.findById(shoppingCartItem.getId())).thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(cartDto);
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(shoppingCartItem);

        ShoppingCartDto result = productQuantityItemUpdater.update(shoppingCartItem.getId(), productQuantityChange);

        assertEquals(cartDto, result);
        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartItemRepository).findById(shoppingCartItem.getId());
        verify(shoppingCartProvider, times(2)).getByUserId(userDto.getId());
        verify(shoppingCartItemRepository).save(shoppingCartItem);
    }

    @Test
    @DisplayName("Update should throw InvalidShoppingCartIdException when item belongs to a different cart")
    void shouldThrowInvalidShoppingCartIdExceptionWhenShoppingCartIdIsInvalid() {
        int productQuantityChange = 5;
        ShoppingCartItem item = CartDtoTestStub.createShoppingCartItem();
        UUID itemId = item.getId();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto shoppingCart = new ShoppingCartDto();
        shoppingCart.setId(UUID.randomUUID()); // different from item's cart id
        shoppingCart.setUserId(userDto.getId());

        when(securityPrincipalProvider.getUserId()).thenReturn(userDto.getId());
        when(shoppingCartItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(shoppingCart);

        Assertions.assertThrows(InvalidShoppingCartIdException.class, () -> productQuantityItemUpdater.update(itemId, productQuantityChange));

        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartItemRepository).findById(itemId);
        verify(shoppingCartProvider).getByUserId(userDto.getId());
        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity becomes zero")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityBecomesZero() {
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem(); // productQuantity = 5
        int productQuantityChange = -5; // 5 + (-5) = 0
        UUID shoppingCartItemId = shoppingCartItem.getId();
        UUID userId = UUID.randomUUID();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartItemRepository.findById(shoppingCartItemId)).thenReturn(Optional.of(shoppingCartItem));

        assertThrows(
                InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItemId, productQuantityChange)
        );

        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartItemRepository).findById(shoppingCartItemId);
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity becomes negative")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityBecomesNegative() {
        int productQuantityChange = -10;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID shoppingCartItemId = shoppingCartItem.getId();
        UUID userId = UUID.randomUUID();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartItemRepository.findById(shoppingCartItemId)).thenReturn(Optional.of(shoppingCartItem));

        assertThrows(
                InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItemId, productQuantityChange)
        );

        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartItemRepository).findById(shoppingCartItemId);
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity change is zero")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityChangeIsZero() {
        int productQuantityChange = 0;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID shoppingCartItemId = shoppingCartItem.getId();
        UUID userId = UUID.randomUUID();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartItemRepository.findById(shoppingCartItemId)).thenReturn(Optional.of(shoppingCartItem));

        assertThrows(
                InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItemId, productQuantityChange)
        );

        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartItemRepository).findById(shoppingCartItemId);
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
    }

    @Test
    @DisplayName("Should throw ShoppingCartItemNotFoundException when shopping cart item does not exist")
    void shouldThrowShoppingCartItemNotFoundExceptionWhenShoppingCartItemNotFound() {
        UUID nonExistedShoppingCartItemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartItemRepository.findById(nonExistedShoppingCartItemId))
                .thenThrow(new ShoppingCartItemNotFoundException(nonExistedShoppingCartItemId));

        assertThrows(ShoppingCartItemNotFoundException.class,
                () -> productQuantityItemUpdater.update(nonExistedShoppingCartItemId, 1));

        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartItemRepository).findById(nonExistedShoppingCartItemId);
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
    }
}
