package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.InvalidItemProductQuantityException;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.stub.CartDtoTestUtil;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.stub.UserDtoTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
@Transactional
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
    @DisplayName("update should return the ShoppingSessionDto with new productQuantity when the productQuantityChange is valid")
    public void update_shouldReturnUpdateShoppingSessionDtoWithValidProductQuantityChange() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
        int productQuantityChange = 5;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestUtil.createShoppingSessionItem();
        UserDto userDto = UserDtoTestUtil.createUserDto();
        ShoppingSessionDto shoppingSessionDto = CartDtoTestUtil.createShoppingSessionDto();
        ShoppingSessionItem updatedShoppingSessionItem = CartDtoTestUtil.createShoppingSessionItem();
        updatedShoppingSessionItem.setProductQuantity(shoppingSessionItem.getProductQuantity() + productQuantityChange);
        updatedShoppingSessionItem.setShoppingSession(new ShoppingSession());

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenReturn(Optional.of(shoppingSessionItem));
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(shoppingSessionDto);
        when(shoppingSessionItemRepository.save(shoppingSessionItem)).thenReturn(updatedShoppingSessionItem);

        ShoppingSessionDto result = productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);

        assertEquals(result, shoppingSessionDto);

        verify(shoppingSessionItemRepository).findById(shoppingSessionItem.getId());
        verify(shoppingSessionProvider).getByUserId(userDto.getId());
        verify(securityPrincipalProvider).get();
        verify(shoppingSessionItemRepository).save(shoppingSessionItem);
    }

    @Test
    @DisplayName("update should return the ShoppingSessionDto with new productQuantity when the productQuantityChange is less than zero")
    public void update_shouldReturnUpdateShoppingSessionDtoWithProductQuantityChangeLessThanZero() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
        int productQuantityChange = -5;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestUtil.createShoppingSessionItem();
        UserDto userDto = UserDtoTestUtil.createUserDto();
        ShoppingSessionDto shoppingSessionDto = CartDtoTestUtil.createShoppingSessionDto();
        ShoppingSessionItem updatedShoppingSessionItem = CartDtoTestUtil.createShoppingSessionItem();
        updatedShoppingSessionItem.setProductQuantity(shoppingSessionItem.getProductQuantity() + productQuantityChange);
        updatedShoppingSessionItem.setShoppingSession(new ShoppingSession());

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenReturn(Optional.of(shoppingSessionItem));
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(shoppingSessionDto);
        when(shoppingSessionItemRepository.save(shoppingSessionItem)).thenReturn(updatedShoppingSessionItem);

        ShoppingSessionDto result = productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);

        assertEquals(result, shoppingSessionDto);

        verify(shoppingSessionItemRepository).findById(shoppingSessionItem.getId());
        verify(shoppingSessionProvider).getByUserId(userDto.getId());
        verify(securityPrincipalProvider).get();
        verify(shoppingSessionItemRepository).save(shoppingSessionItem);
    }

    @Test
    @DisplayName("update should throw InvalidShoppingSessionIdException when shopping session id is invalid")
    public void update_ShouldThrowInvalidShoppingSessionIdExceptionWhenShoppingSessionIdIsInvalid() throws ShoppingSessionNotFoundException, InvalidShoppingSessionIdException {
        int productQuantityChange = 5;
        ShoppingSessionItem item = CartDtoTestUtil.createShoppingSessionItem();
        UserDto userDto = UserDtoTestUtil.createUserDto();
        ShoppingSessionDto shoppingSession = new ShoppingSessionDto();
        shoppingSession.setId(UUID.randomUUID());
        shoppingSession.setUserId(userDto.getId());

        when(shoppingSessionItemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(shoppingSession);
        when(shoppingSessionItemRepository.save(item)).thenReturn(item);

        assertThrows(InvalidShoppingSessionIdException.class, () -> {
            productQuantityItemUpdater.update(item.getId(), productQuantityChange);
        });
    }

    @Test
    @DisplayName("findById should throw InvalidItemProductQuantityException when attempted to set negative products quantity for item")
    public void findById_ShouldThrowInvalidItemProductQuantityExceptionWhenProductQuantitySetNegative() {
        int productQuantityChange = -10;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestUtil.createShoppingSessionItem();

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenThrow(new InvalidItemProductQuantityException(productQuantityChange));

        assertThrows(InvalidItemProductQuantityException.class, () -> {
            productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);
        });
    }

    @Test
    @DisplayName("findById should throw InvalidItemProductQuantityException when attempted to sent zero products quantity change")
    public void findById_ShouldThrowInvalidItemProductQuantityExceptionWhenProductQuantityChangeIsZero() {
        int productQuantityChange = 0;
        ShoppingSessionItem shoppingSessionItem = CartDtoTestUtil.createShoppingSessionItem();

        when(shoppingSessionItemRepository.findById(shoppingSessionItem.getId())).thenThrow(new InvalidItemProductQuantityException(productQuantityChange));

        assertThrows(InvalidItemProductQuantityException.class, () -> {
            productQuantityItemUpdater.update(shoppingSessionItem.getId(), productQuantityChange);
        });
    }

    @Test
    @DisplayName("findById should throw ShoppingSessionItemNotFoundException when shopping session item does not found")
    public void findById_ShouldThrowShoppingSessionItemNotFoundExceptionWhenShoppingSessionItemNotFound() {
        UUID nonExistedShoppingSessionItemId = UUID.randomUUID();

        when(shoppingSessionItemRepository.findById(nonExistedShoppingSessionItemId)).thenThrow(new ShoppingSessionItemNotFoundException(nonExistedShoppingSessionItemId));

        assertThrows(ShoppingSessionItemNotFoundException.class, () -> {
            productQuantityItemUpdater.update(nonExistedShoppingSessionItemId, 0);
        });
    }
}