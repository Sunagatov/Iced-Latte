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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQuantityItemUpdaterTest {

    @Mock
    private ShoppingCartItemRepository shoppingCartItemRepository;

    @Mock
    private ShoppingCartProvider shoppingCartProvider;

    @InjectMocks
    private ProductQuantityItemUpdater productQuantityItemUpdater;

    @Test
    @DisplayName("Update should return fresh ShoppingCartDto after save when productQuantityChange is valid")
    void shouldReturnUpdateShoppingCartDtoWithValidProductQuantityChange() throws ShoppingCartNotFoundException, InvalidShoppingCartIdException {
        int productQuantityChange = 5;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto cartDto = CartDtoTestStub.createShoppingCartDto();
        cartDto.setId(shoppingCartItem.getShoppingCart().getId());

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userDto.getId()))
                .thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(shoppingCartItem);
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(cartDto);

        ShoppingCartDto result = productQuantityItemUpdater.update(shoppingCartItem.getId(), userDto.getId(), productQuantityChange);

        assertEquals(cartDto, result);
        verify(shoppingCartItemRepository).findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userDto.getId());
        verify(shoppingCartItemRepository).save(shoppingCartItem);
        verify(shoppingCartProvider).getByUserId(userDto.getId());
    }

    @Test
    @DisplayName("Update should return fresh ShoppingCartDto after save when productQuantityChange is negative but result >= 1")
    void shouldReturnUpdateShoppingCartDtoWithProductQuantityChangeLessThanZero() throws ShoppingCartNotFoundException, InvalidShoppingCartIdException {
        int productQuantityChange = -2; // item has quantity 5, result = 3
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingCartDto cartDto = CartDtoTestStub.createShoppingCartDto();
        cartDto.setId(shoppingCartItem.getShoppingCart().getId());

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userDto.getId()))
                .thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(shoppingCartItem);
        when(shoppingCartProvider.getByUserId(userDto.getId())).thenReturn(cartDto);

        ShoppingCartDto result = productQuantityItemUpdater.update(shoppingCartItem.getId(), userDto.getId(), productQuantityChange);

        assertEquals(cartDto, result);
        verify(shoppingCartItemRepository).findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userDto.getId());
        verify(shoppingCartItemRepository).save(shoppingCartItem);
        verify(shoppingCartProvider).getByUserId(userDto.getId());
    }

    @Test
    @DisplayName("Update should throw ShoppingCartItemNotFoundException when item belongs to a different user (404, not 400)")
    void shouldThrowNotFoundWhenItemBelongsToDifferentUser() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(itemId, userId))
                .thenReturn(Optional.empty());

        assertThrows(ShoppingCartItemNotFoundException.class,
                () -> productQuantityItemUpdater.update(itemId, userId, 1));

        verify(shoppingCartItemRepository).findByIdAndShoppingCartUserId(itemId, userId);
        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity becomes zero")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityBecomesZero() {
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem(); // productQuantity = 5
        int productQuantityChange = -5; // 5 + (-5) = 0
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userId))
                .thenReturn(Optional.of(shoppingCartItem));

        assertThrows(InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItem.getId(), userId, productQuantityChange));

        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity becomes negative")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityBecomesNegative() {
        int productQuantityChange = -10;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userId))
                .thenReturn(Optional.of(shoppingCartItem));

        assertThrows(InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItem.getId(), userId, productQuantityChange));

        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw InvalidItemProductQuantityException when quantity change is zero")
    void shouldThrowInvalidItemProductQuantityExceptionWhenQuantityChangeIsZero() {
        int productQuantityChange = 0;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userId))
                .thenReturn(Optional.of(shoppingCartItem));

        assertThrows(InvalidItemProductQuantityException.class,
                () -> productQuantityItemUpdater.update(shoppingCartItem.getId(), userId, productQuantityChange));

        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw ShoppingCartItemNotFoundException when shopping cart item does not exist")
    void shouldThrowShoppingCartItemNotFoundExceptionWhenShoppingCartItemNotFound() {
        UUID nonExistedShoppingCartItemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(nonExistedShoppingCartItemId, userId))
                .thenReturn(Optional.empty());

        assertThrows(ShoppingCartItemNotFoundException.class,
                () -> productQuantityItemUpdater.update(nonExistedShoppingCartItemId, userId, 1));

        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
        verify(shoppingCartProvider, never()).getByUserId(any(UUID.class));
    }
}
