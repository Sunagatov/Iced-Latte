package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private ShoppingCartCreator shoppingCartCreator;

    @Test
    @DisplayName("Add should return the ShoppingCartDto with increased list of items when the itemsToAdd set is valid")
    void shouldItemsAddToShoppingCartDtoWithValidItemsSet() {
        UUID userId = UUID.randomUUID();

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setId(UUID.randomUUID());
        shoppingCart.setItems(new HashSet<>());

        Set<NewShoppingCartItemDto> newShoppingCartItemDtoToAdd = Set.of(CartDtoTestStub.createShoppingCartItemDtoToAdd());

        ShoppingCartItem itemToAdd = CartDtoTestStub.createShoppingCartItem();

        ShoppingCart updatedShoppingCart = new ShoppingCart();
        updatedShoppingCart.setId(shoppingCart.getId());
        updatedShoppingCart.setItems(Set.of(itemToAdd));

        ShoppingCartDto expectedShoppingCartDto = new ShoppingCartDto();
        expectedShoppingCartDto.setItems(List.of(CartDtoTestStub.createShoppingCartItemDto()));

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartCreator.getOrCreate(userId)).thenReturn(shoppingCart);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(itemToAdd.getProductInfo()));
        when(shoppingCartRepository.save(shoppingCart)).thenReturn(updatedShoppingCart);
        when(shoppingCartDtoConverter.toDto(updatedShoppingCart)).thenReturn(expectedShoppingCartDto);

        ShoppingCartDto result = addItemsToShoppingCartHelper.add(newShoppingCartItemDtoToAdd);

        assertEquals(expectedShoppingCartDto, result);

        verify(securityPrincipalProvider).getUserId();
        verify(shoppingCartCreator).getOrCreate(userId);
        verify(shoppingCartRepository).save(shoppingCart);
        verify(shoppingCartDtoConverter).toDto(updatedShoppingCart);
    }

    @Test
    @DisplayName("Add should increase quantity for an item that already exists in the shopping cart")
    void shouldIncreaseQuantityForExistingItemWhenProductAlreadyExistsInCart() {
        UUID userId = UUID.randomUUID();
        UUID existingProductId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        UUID newProductId = UUID.fromString("b58ac6f1-7ee1-4888-9055-3bebb6aa3632");

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setId(UUID.randomUUID());
        shoppingCart.setItemsQuantity(1);
        shoppingCart.setProductsQuantity(1);

        ProductInfo existingProduct = new ProductInfo(
                existingProductId, 1L, "Existing coffee", "Existing description", BigDecimal.valueOf(2.5), 10, true,
                BigDecimal.ZERO, 0, "brandName", "sellerName", "originCountry", 100, 10, 4, 25, 200, 20,
                LocalDateTime.now(), 60, null
        );

        ShoppingCartItem existingItem = new ShoppingCartItem(
                UUID.randomUUID(), shoppingCart, existingProduct, 1
        );

        shoppingCart.setItems(new HashSet<>(Set.of(existingItem)));

        NewShoppingCartItemDto existingProductToAdd = new NewShoppingCartItemDto();
        existingProductToAdd.setProductId(existingProductId);
        existingProductToAdd.setProductQuantity(2);

        NewShoppingCartItemDto newProductToAdd = new NewShoppingCartItemDto();
        newProductToAdd.setProductId(newProductId);
        newProductToAdd.setProductQuantity(3);

        Set<NewShoppingCartItemDto> itemsToAdd = Set.of(existingProductToAdd, newProductToAdd);

        ProductInfo newProduct = new ProductInfo(
                newProductId, 1L, "New coffee", "New description", BigDecimal.valueOf(3.5), 8, true,
                BigDecimal.ZERO, 0, "brandName", "sellerName", "originCountry", 100, 10, 4, 25, 200, 20,
                LocalDateTime.now(), 60, null
        );

        ShoppingCartDto expectedShoppingCartDto = new ShoppingCartDto();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartCreator.getOrCreate(userId)).thenReturn(shoppingCart);
        when(productInfoRepository.findAllById(Set.of(newProductId))).thenReturn(List.of(newProduct));
        when(shoppingCartRepository.save(shoppingCart)).thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingCartDtoConverter.toDto(shoppingCart)).thenReturn(expectedShoppingCartDto);

        ShoppingCartDto result = addItemsToShoppingCartHelper.add(itemsToAdd);

        assertEquals(expectedShoppingCartDto, result);
        assertEquals(2, shoppingCart.getItemsQuantity());
        assertEquals(6, shoppingCart.getProductsQuantity());

        ShoppingCartItem updatedExistingItem = shoppingCart.getItems().stream()
                .filter(item -> item.getProductInfo().getId().equals(existingProductId))
                .findFirst()
                .orElseThrow();

        assertEquals(3, updatedExistingItem.getProductQuantity());
        assertEquals(2, shoppingCart.getItems().size());

        verify(productInfoRepository).findAllById(Set.of(newProductId));
        verify(shoppingCartRepository).save(shoppingCart);
    }
}
