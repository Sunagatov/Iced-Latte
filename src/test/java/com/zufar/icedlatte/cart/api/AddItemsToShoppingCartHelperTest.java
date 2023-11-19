package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddItemsToShoppingCartHelperTest {

    @InjectMocks
    private AddItemsToShoppingCartHelper addItemsToShoppingCartHelper;

    @Mock
    private ShoppingCartRepository shoppingCartRepository;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Mock
    private ProductInfoRepository productInfoRepository;

    @Mock
    private ShoppingCartDtoConverter shoppingCartDtoConverter;

    @Test
    @DisplayName("Add should return the ShoppingCartDto with increased list of items when the itemsToAdd set is valid")
    void shouldItemsAddToShoppingCartDtoWithValidItemsSet() {
        UUID userId = UUID.randomUUID();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setId(UUID.randomUUID());
        shoppingCart.setItems(new HashSet<>());

        Set<NewShoppingCartItemDto> newShoppingCartItemDtoToAdd = Collections.singleton(CartDtoTestStub.createShoppingCartItemDtoToAdd());

        ShoppingCartItem itemToAdd = CartDtoTestStub.createShoppingCartItem();

        ShoppingCart updatedShoppingCart = new ShoppingCart();
        updatedShoppingCart.setId(shoppingCart.getId());
        updatedShoppingCart.setItems(Collections.singleton(itemToAdd));

        ShoppingCartDto expectedShoppingCartDto = new ShoppingCartDto();
        expectedShoppingCartDto.setItems(Collections.singletonList(CartDtoTestStub.createShoppingCartItemDto()));

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(shoppingCart);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(itemToAdd.getProductInfo()));
        when(shoppingCartRepository.save(shoppingCart)).thenReturn(updatedShoppingCart);
        when(shoppingCartDtoConverter.toDto(updatedShoppingCart)).thenReturn(expectedShoppingCartDto);

        ShoppingCartDto result = addItemsToShoppingCartHelper.add(newShoppingCartItemDtoToAdd);

        assertEquals(result, expectedShoppingCartDto);

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(shoppingCartRepository, times(1)).findShoppingCartByUserId(userId);
        verify(shoppingCartRepository, times(1)).save(shoppingCart);
        verify(shoppingCartDtoConverter, times(1)).toDto(updatedShoppingCart);

    }
}
