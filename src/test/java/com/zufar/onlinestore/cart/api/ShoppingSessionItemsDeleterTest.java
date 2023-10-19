package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ShoppingSessionItemsDeleterTest {
    @InjectMocks
    private ShoppingSessionItemsDeleter shoppingSessionItemsDeleter;

    @Mock
    private ShoppingSessionItemRepository shoppingSessionItemRepository;

    @Mock
    private ShoppingSessionProvider shoppingSessionProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    ShoppingSessionItemDto item1;

    ShoppingSessionItemDto item2;

    List<ShoppingSessionItemDto> initialItems;

    UUID userId;

    UUID item1Id;

    UUID item2Id;

    @BeforeEach
    public void setUp() {
        userId = UUID.randomUUID();
        item1Id = UUID.randomUUID();
        item2Id = UUID.randomUUID();

        item1 = mock(ShoppingSessionItemDto.class);
        when(item1.id()).thenReturn(item1Id);

        item2 = mock(ShoppingSessionItemDto.class);
        when(item1.id()).thenReturn(item2Id);

        initialItems = List.of(item1, item2);

        when(securityPrincipalProvider.get()).thenReturn(UserDto.builder().userId(userId).build());
    }

    @Test
    public void shouldItemsDeleteFromShoppingSessionWithValidItemsList() {
        List<UUID> itemIds = List.of(item2Id);
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest(itemIds);

        ShoppingSessionDto initialShoppingSessionDto = mock(ShoppingSessionDto.class);
        when(initialShoppingSessionDto.items()).thenReturn(initialItems);

        List<ShoppingSessionItemDto> expectedItems = List.of(item1);

        ShoppingSessionDto expectedShoppingSessionDto = mock(ShoppingSessionDto.class);
        when(expectedShoppingSessionDto.items()).thenReturn(expectedItems);

        when(shoppingSessionProvider.getByUserId(userId)).thenReturn(expectedShoppingSessionDto);

        ShoppingSessionDto result = shoppingSessionItemsDeleter.delete(request);

        assertEquals(expectedShoppingSessionDto, result);
        assertNotEquals(initialShoppingSessionDto, result);

        verify(shoppingSessionItemRepository).deleteAllByIdInBatch(itemIds);
    }

    @Test
    public void shouldItemsNotDeleteFromShoppingSessionWithInvalidItemsList() {
        UUID item3Id = UUID.randomUUID();
        List<UUID> itemIds = List.of(item3Id);
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest(itemIds);

        ShoppingSessionDto initialShoppingSessionDto = mock(ShoppingSessionDto.class);
        when(initialShoppingSessionDto.items()).thenReturn(initialItems);

        when(shoppingSessionProvider.getByUserId(userId)).thenReturn(initialShoppingSessionDto);

        ShoppingSessionDto result = shoppingSessionItemsDeleter.delete(request);

        assertEquals(initialShoppingSessionDto, result);
    }

    @Test
    public void shouldDeleteAllItemsFromShoppingSessionWithValidItemsList() {
        List<UUID> itemIds = List.of(item1Id, item2Id);
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest(itemIds);

        ShoppingSessionDto initialShoppingSessionDto = mock(ShoppingSessionDto.class);
        when(initialShoppingSessionDto.items()).thenReturn(initialItems);

        List<ShoppingSessionItemDto> expectedItems = List.of();

        ShoppingSessionDto expectedShoppingSessionDto = mock(ShoppingSessionDto.class);
        when(expectedShoppingSessionDto.items()).thenReturn(expectedItems);

        when(shoppingSessionProvider.getByUserId(userId)).thenReturn(expectedShoppingSessionDto);

        ShoppingSessionDto result = shoppingSessionItemsDeleter.delete(request);

        assertTrue(result.items().isEmpty());
    }
}
