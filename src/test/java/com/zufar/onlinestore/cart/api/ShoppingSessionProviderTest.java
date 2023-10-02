package com.zufar.onlinestore.cart.api;
import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ShoppingSessionProviderTest {

    @Mock
    private ShoppingSessionRepository shoppingSessionRepository;

    @Mock
    private ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @InjectMocks
    private ShoppingSessionProvider shoppingSessionProvider;

    @Test
    void shouldReturnShoppingSessionDtoWhenUserIdExists() {
        UUID userid = UUID.randomUUID();

        when(shoppingSessionRepository.findShoppingSessionByUserId(userid)).thenReturn(mock(ShoppingSession.class));
        when(shoppingSessionDtoConverter.toDto(any(ShoppingSession.class))).thenReturn(mock(ShoppingSessionDto.class));

        ShoppingSessionDto resultDto = shoppingSessionProvider.getByUserId(userid);

        assertNotNull(resultDto);

        verify(shoppingSessionDtoConverter, times(1)).toDto(any(ShoppingSession.class));
    }

    @Test
    void shouldThrowExceptionWhenUserIdNotExists() {
        UUID userId = UUID.randomUUID();

        when(shoppingSessionRepository.findById(userId)).thenReturn(Optional.empty());

        ShoppingSessionNotFoundException thrownException = assertThrows(
                ShoppingSessionNotFoundException.class,
                () -> shoppingSessionProvider.getByUserId(userId)
        );

        assertEquals(
                String.format("The shopping session for the user with id = %s is not found.", userId),
                thrownException.getMessage()
        );

        verify(shoppingSessionRepository, times(1)).findShoppingSessionByUserId(userId);
    }
}
