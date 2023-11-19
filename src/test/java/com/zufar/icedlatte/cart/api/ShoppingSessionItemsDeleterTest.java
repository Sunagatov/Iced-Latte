package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.cart.repository.ShoppingSessionItemRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.openapi.dto.UserDto;
import com.zufar.icedlatte.user.stub.UserDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingSessionItemsDeleterTest {

    @InjectMocks
    private ShoppingSessionItemsDeleter shoppingSessionItemsDeleter;

    @Mock
    private ShoppingSessionItemRepository shoppingSessionItemRepository;

    @Mock
    private ShoppingSessionProvider shoppingSessionProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Test
    @DisplayName("Delete should return the ShoppingSessionDto with correct list of items when the itemIdsForDelete list is valid")
    void shouldItemsDeleteFromShoppingSessionDtoWithValidItemsList() {
        List<UUID> itemIdsForDelete = Collections.singletonList(UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest();
        request.shoppingSessionItemIds(itemIdsForDelete);
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingSessionDto expectedResult = CartDtoTestStub.createShoppingSessionDto();
        expectedResult.setItems(CartDtoTestStub.createShoppingSessionDtoList());

        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(expectedResult);

        ShoppingSessionDto actualResult = shoppingSessionItemsDeleter.delete(request);

        assertEquals(expectedResult, actualResult);

        verify(shoppingSessionItemRepository, times(1)).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingSessionProvider, times(1)).getByUserId(userDto.getId());
    }

    @Test
    @DisplayName("Delete should return the ShoppingSessionDto with empty list of items when the itemIdsForDelete list is valid")
    void shouldDeleteAllItemsFromShoppingSessionDtoWithValidItemsList() {
        List<UUID> itemIdsForDelete = List.of(
                UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddc"),
                UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241"),
                UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest();
        request.shoppingSessionItemIds(itemIdsForDelete);
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingSessionDto expectedResult = CartDtoTestStub.createEmptyShoppingSessionDto();

        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(expectedResult);

        ShoppingSessionDto actualResult = shoppingSessionItemsDeleter.delete(request);

        assertEquals(expectedResult, actualResult);

        verify(shoppingSessionItemRepository, times(1)).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingSessionProvider, times(1)).getByUserId(userDto.getId());
    }

    @Test
    @DisplayName("Delete should return the ShoppingSessionDto with list of items without change when the itemIdsForDelete list is valid")
    void shouldDeleteNothingFromShoppingSessionDtoWithEmptyItemsList() {
        List<UUID> itemIdsForDelete = new ArrayList<>();
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest();
        request.shoppingSessionItemIds(itemIdsForDelete);
        UserDto userDto = UserDtoTestStub.createUserDto();
        ShoppingSessionDto expectedResult = CartDtoTestStub.createFullShoppingSessionDto();
        expectedResult.setItems(CartDtoTestStub.createFullShoppingSessionDtoList());

        when(securityPrincipalProvider.get()).thenReturn(userDto);
        when(shoppingSessionProvider.getByUserId(userDto.getId())).thenReturn(expectedResult);

        ShoppingSessionDto actualResult = shoppingSessionItemsDeleter.delete(request);

        assertEquals(expectedResult, actualResult);

        verify(shoppingSessionItemRepository, times(1)).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider, times(1)).get();
        verify(shoppingSessionProvider, times(1)).getByUserId(userDto.getId());
    }
}
