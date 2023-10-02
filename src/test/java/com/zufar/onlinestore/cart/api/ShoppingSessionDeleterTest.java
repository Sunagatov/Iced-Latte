package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.DeleteItemsFromShoppingSessionRequest;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ShoppingSessionDeleterTest {

    @Mock
    private ShoppingSessionItemRepository shoppingSessionItemRepository;

    @Mock
    private ShoppingSessionProvider shoppingSessionProvider;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @InjectMocks
    private ShoppingSessionItemsDeleter shoppingSessionItemsDeleter;

    @Test
    void shouldReturnShoppingSessionDtoAfterDeleteListOfItems() {
        UUID userid = UUID.randomUUID();
        DeleteItemsFromShoppingSessionRequest request = new DeleteItemsFromShoppingSessionRequest(Collections.emptyList());

        when(securityPrincipalProvider.get()).thenReturn(UserDto.builder().userId(userid).build());
        when(shoppingSessionProvider.getByUserId(userid)).thenReturn(mock(ShoppingSessionDto.class));

        ShoppingSessionDto resultDto = shoppingSessionItemsDeleter.delete(request);

        assertNotNull(resultDto);

        verify(shoppingSessionItemRepository, times(1)).deleteAllByIdInBatch(request.shoppingSessionItemIds());
    }
}
