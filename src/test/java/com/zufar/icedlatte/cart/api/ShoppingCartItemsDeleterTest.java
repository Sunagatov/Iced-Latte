package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
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
class ShoppingCartItemsDeleterTest {

    @InjectMocks
    private ShoppingCartItemsDeleter shoppingCartItemsDeleter;

    @Mock
    private ShoppingCartItemRepository shoppingCartItemRepository;

    @Mock
    private ShoppingCartProvider shoppingCartProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Test
    @DisplayName("Delete should return the ShoppingCartDto with correct list of items when the itemIdsForDelete list is valid")
    void shouldItemsDeleteFromShoppingCartDtoWithValidItemsList() {
        UUID userId = UUID.randomUUID();
        List<UUID> itemIdsForDelete = Collections.singletonList(UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        DeleteItemsFromShoppingCartRequest request = new DeleteItemsFromShoppingCartRequest();
        request.shoppingCartItemIds(itemIdsForDelete);
        ShoppingCartDto expectedResult = CartDtoTestStub.createShoppingCartDto();
        expectedResult.setItems(CartDtoTestStub.createShoppingCartDtoList());

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartProvider.getByUserId(userId)).thenReturn(expectedResult);

        ShoppingCartDto actualResult = shoppingCartItemsDeleter.delete(request);

        assertEquals(expectedResult, actualResult);
        verify(shoppingCartItemRepository, times(1)).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(shoppingCartProvider, times(1)).getByUserId(userId);
    }

    @Test
    @DisplayName("Delete should return the ShoppingCartDto with empty list of items when the itemIdsForDelete list is valid")
    void shouldDeleteAllItemsFromShoppingCartDtoWithValidItemsList() {
        UUID userId = UUID.randomUUID();
        List<UUID> itemIdsForDelete = List.of(
                UUID.fromString("9b588163-b781-46bf-8714-bd0145337ddc"),
                UUID.fromString("e5cadeb1-089c-430f-85d1-e18438167241"),
                UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        DeleteItemsFromShoppingCartRequest request = new DeleteItemsFromShoppingCartRequest();
        request.shoppingCartItemIds(itemIdsForDelete);
        ShoppingCartDto expectedResult = CartDtoTestStub.createEmptyShoppingCartDto();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartProvider.getByUserId(userId)).thenReturn(expectedResult);

        ShoppingCartDto actualResult = shoppingCartItemsDeleter.delete(request);

        assertEquals(expectedResult, actualResult);
        verify(shoppingCartItemRepository, times(1)).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(shoppingCartProvider, times(1)).getByUserId(userId);
    }

    @Test
    @DisplayName("Delete should return the ShoppingCartDto with list of items without change when the itemIdsForDelete list is valid")
    void shouldDeleteNothingFromShoppingCartDtoWithEmptyItemsList() {
        UUID userId = UUID.randomUUID();
        List<UUID> itemIdsForDelete = new ArrayList<>();
        DeleteItemsFromShoppingCartRequest request = new DeleteItemsFromShoppingCartRequest();
        request.shoppingCartItemIds(itemIdsForDelete);
        ShoppingCartDto expectedResult = CartDtoTestStub.createFullShoppingCartDto();
        expectedResult.setItems(CartDtoTestStub.createFullShoppingCartDtoList());

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartProvider.getByUserId(userId)).thenReturn(expectedResult);

        ShoppingCartDto actualResult = shoppingCartItemsDeleter.delete(request);

        assertEquals(expectedResult, actualResult);
        verify(shoppingCartItemRepository, times(1)).deleteAllByIdInBatch(itemIdsForDelete);
        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(shoppingCartProvider, times(1)).getByUserId(userId);
    }
}
