package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.stub.CartDtoTestUtil;
import com.zufar.onlinestore.openapi.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Transactional
public class AddItemsToShoppingSessionHelperTest {

    @InjectMocks
    private AddItemsToShoppingSessionHelper addItemsToShoppingSessionHelper;

    @Mock
    private ShoppingSessionRepository shoppingSessionRepository;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Test
    @DisplayName("add should return the ShoppingSessionDto with increased list of items when the itemsToAdd set is valid")
    public void add_shouldItemsAddToShoppingSessionDtoWithValidItemsSet() {
        UUID userId = UUID.randomUUID();

        ShoppingSession shoppingSession = new ShoppingSession();
        shoppingSession.setId(UUID.randomUUID());
        shoppingSession.setItems(new HashSet<>());

        Set<NewShoppingSessionItemDto> newShoppingSessionItemDtoToAdd = Set.of(CartDtoTestUtil.createShoppingSessionItemDtoToAdd());

        ShoppingSessionItem itemToAdd = CartDtoTestUtil.createShoppingSessionItem();

        ShoppingSession updatedShoppingSession = new ShoppingSession();
        updatedShoppingSession.setId(shoppingSession.getId());
        updatedShoppingSession.setItems(Set.of(itemToAdd));

        ShoppingSessionDto expectedShoppingSessionDto = new ShoppingSessionDto();
        expectedShoppingSessionDto.setItems(List.of(CartDtoTestUtil.createShoppingSessionItemDto()));

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingSessionRepository.findShoppingSessionByUserId(userId)).thenReturn(shoppingSession);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(itemToAdd.getProductInfo()));
        when(shoppingSessionRepository.save(shoppingSession)).thenReturn(updatedShoppingSession);
        when(shoppingSessionDtoConverter.toDto(updatedShoppingSession)).thenReturn(expectedShoppingSessionDto);

        ShoppingSessionDto result = addItemsToShoppingSessionHelper.add(newShoppingSessionItemDtoToAdd);

        assertEquals(result, expectedShoppingSessionDto);

        verify(securityPrincipalProvider).getUserId();
        verify(shoppingSessionRepository).findShoppingSessionByUserId(userId);
        verify(shoppingSessionRepository).save(shoppingSession);
        verify(shoppingSessionDtoConverter).toDto(updatedShoppingSession);

    }
}
