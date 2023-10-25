package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.stub.CartDtoTestUtil;
import com.zufar.onlinestore.openapi.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.openapi.dto.UserDto;
import com.zufar.onlinestore.user.stub.UserDtoTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Transactional
public class ShoppingSessionItemsDeleterTest {

    @InjectMocks
    private ShoppingSessionItemsDeleter shoppingSessionItemsDeleter;

    @Mock
    private ShoppingSessionItemRepository shoppingSessionItemRepository;

    @Mock
    private ShoppingSessionProvider shoppingSessionProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Test
    @DisplayName("delete should return the ShoppingSessionDto with correct list of items when the itemIdsForDelete list is valid")
    public void delete_shouldItemsDeleteFromShoppingSessionDtoWithValidItemsList() {
        List<UUID> itemIdsForDelete = List.of(UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest();
        request.shoppingSessionItemIds(itemIdsForDelete);
        UserDto userDto = UserDtoTestUtil.createUserDto();
        ShoppingSessionDto expectedShoppingSessionDto = CartDtoTestUtil.createShoppingSessionDto();

        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(expectedShoppingSessionDto);

        ShoppingSessionDto result = shoppingSessionItemsDeleter.delete(request);

        assertEquals(expectedShoppingSessionDto, result);

        verify(shoppingSessionItemRepository).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider).get();
        verify(shoppingSessionProvider).getByUserId(userDto.getId());
    }

    @Test
    @DisplayName("delete should return the ShoppingSessionDto with empty list of items when the itemIdsForDelete list is valid")
    public void delete_shouldDeleteAllItemsFromShoppingSessionDtoWithValidItemsList() {
        List<UUID> itemIdsForDelete = List.of(
                UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddc"),
                UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241"),
                UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest();
        request.shoppingSessionItemIds(itemIdsForDelete);
        UserDto userDto = UserDtoTestUtil.createUserDto();
        ShoppingSessionDto expectedShoppingSessionDto = CartDtoTestUtil.createEmptyShoppingSessionDto();

        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(expectedShoppingSessionDto);

        ShoppingSessionDto result = shoppingSessionItemsDeleter.delete(request);

        assertEquals(expectedShoppingSessionDto, result);

        verify(shoppingSessionItemRepository).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider).get();
        verify(shoppingSessionProvider).getByUserId(userDto.getId());
    }

    @Test
    @DisplayName("delete should return the ShoppingSessionDto with list of items without change when the itemIdsForDelete list is valid")
    public void delete_shouldDeleteNothingFromShoppingSessionDtoWithEmptyItemsList() {
        List<UUID> itemIdsForDelete = new ArrayList<>();
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest();
        request.shoppingSessionItemIds(itemIdsForDelete);
        UserDto userDto = UserDtoTestUtil.createUserDto();
        ShoppingSessionDto expectedShoppingSessionDto = CartDtoTestUtil.createFullShoppingSessionDto();

        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(expectedShoppingSessionDto);

        ShoppingSessionDto result = shoppingSessionItemsDeleter.delete(request);

        assertEquals(expectedShoppingSessionDto, result);

        verify(shoppingSessionItemRepository).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider).get();
        verify(shoppingSessionProvider).getByUserId(userDto.getId());
    }
}
