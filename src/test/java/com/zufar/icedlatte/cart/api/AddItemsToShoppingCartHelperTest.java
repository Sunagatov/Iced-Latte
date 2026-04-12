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

import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        addItemsToShoppingCartHelper.add(Set.of(existingProductToAdd, newProductToAdd));

        assertEquals(2, shoppingCart.getItems().size());

        ShoppingCartItem updatedExistingItem = shoppingCart.getItems().stream()
                .filter(item -> item.getProductInfo().getId().equals(existingProductId))
                .findFirst()
                .orElseThrow();
        assertEquals(3, updatedExistingItem.getProductQuantity());

        ShoppingCartItem addedNewItem = shoppingCart.getItems().stream()
                .filter(item -> item.getProductInfo().getId().equals(newProductId))
                .findFirst()
                .orElseThrow();
        assertEquals(3, addedNewItem.getProductQuantity());

        int totalProductsQuantity = shoppingCart.getItems().stream()
                .mapToInt(ShoppingCartItem::getProductQuantity)
                .sum();
        assertEquals(6, totalProductsQuantity);

        verify(productInfoRepository).findAllById(Set.of(newProductId));
        verify(shoppingCartRepository).save(shoppingCart);
    }

    @Test
    @DisplayName("Add should recover gracefully when concurrent insert causes a uniqueness conflict")
    void shouldRecoverOnConcurrentItemInsertConflict() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");

        ShoppingCart firstCart = new ShoppingCart();
        firstCart.setId(UUID.randomUUID());
        firstCart.setItems(new HashSet<>());

        ProductInfo product = new ProductInfo(
                productId, 1L, "Coffee", "Desc", BigDecimal.valueOf(2.5), 10, true,
                BigDecimal.ZERO, 0, "brand", "seller", "country", 100, 10, 4, 25, 200, 20,
                LocalDateTime.now(), 60, null
        );

        // After conflict, fresh cart already has the item inserted by the concurrent request
        ShoppingCartItem concurrentItem = new ShoppingCartItem(UUID.randomUUID(), firstCart, product, 1);
        ShoppingCart freshCart = new ShoppingCart();
        freshCart.setId(firstCart.getId());
        freshCart.setItems(new HashSet<>(Set.of(concurrentItem)));

        NewShoppingCartItemDto itemToAdd = new NewShoppingCartItemDto();
        itemToAdd.setProductId(productId);
        itemToAdd.setProductQuantity(2);

        ShoppingCartDto expectedDto = new ShoppingCartDto();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartCreator.getOrCreate(userId)).thenReturn(firstCart).thenReturn(freshCart);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(product));
        // First save throws (concurrent insert conflict on the known constraint), second save succeeds
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenThrow(new DataIntegrityViolationException("uq_shopping_cart_item_cart_product"))
                .thenAnswer(inv -> inv.getArgument(0));
        when(shoppingCartDtoConverter.toDto(freshCart)).thenReturn(expectedDto);

        ShoppingCartDto result = addItemsToShoppingCartHelper.add(Set.of(itemToAdd));

        assertEquals(expectedDto, result);
        // quantity on the concurrent item should have been incremented by 2
        assertEquals(3, concurrentItem.getProductQuantity());
        verify(shoppingCartRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    @DisplayName("Add should recover and still add non-conflicting new products when one product conflicts")
    void shouldRecoverAndAddNonConflictingProductsOnConflict() {
        UUID userId = UUID.randomUUID();
        UUID conflictingProductId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        UUID newProductId = UUID.fromString("b58ac6f1-7ee1-4888-9055-3bebb6aa3632");

        ShoppingCart firstCart = new ShoppingCart();
        firstCart.setId(UUID.randomUUID());
        firstCart.setItems(new HashSet<>());

        ProductInfo conflictingProduct = new ProductInfo(
                conflictingProductId, 1L, "Coffee A", "Desc", BigDecimal.valueOf(2.5), 10, true,
                BigDecimal.ZERO, 0, "brand", "seller", "country", 100, 10, 4, 25, 200, 20,
                LocalDateTime.now(), 60, null
        );
        ProductInfo newProduct = new ProductInfo(
                newProductId, 1L, "Coffee B", "Desc", BigDecimal.valueOf(3.0), 5, true,
                BigDecimal.ZERO, 0, "brand", "seller", "country", 100, 10, 4, 25, 200, 20,
                LocalDateTime.now(), 60, null
        );

        // Fresh cart is independent — only has the conflicting product (inserted by concurrent request)
        ShoppingCart freshCart = new ShoppingCart();
        freshCart.setId(firstCart.getId());
        ShoppingCartItem concurrentItem = new ShoppingCartItem(UUID.randomUUID(), freshCart, conflictingProduct, 1);
        freshCart.setItems(new HashSet<>(Set.of(concurrentItem)));

        NewShoppingCartItemDto conflictingItemDto = new NewShoppingCartItemDto();
        conflictingItemDto.setProductId(conflictingProductId);
        conflictingItemDto.setProductQuantity(2);

        NewShoppingCartItemDto newItemDto = new NewShoppingCartItemDto();
        newItemDto.setProductId(newProductId);
        newItemDto.setProductQuantity(3);

        ShoppingCartDto expectedDto = new ShoppingCartDto();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartCreator.getOrCreate(userId)).thenReturn(firstCart).thenReturn(freshCart);
        when(productInfoRepository.findAllById(any()))
                .thenAnswer(inv -> {
                    Set<UUID> requested = new HashSet<>(inv.getArgument(0));
                    return Stream.of(conflictingProduct, newProduct)
                            .filter(p -> requested.contains(p.getId()))
                            .toList();
                });
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenThrow(new DataIntegrityViolationException("uq_shopping_cart_item_cart_product"))
                .thenAnswer(inv -> inv.getArgument(0));
        when(shoppingCartDtoConverter.toDto(freshCart)).thenReturn(expectedDto);

        addItemsToShoppingCartHelper.add(Set.of(conflictingItemDto, newItemDto));

        // conflicting product quantity incremented from 1 to 3
        assertEquals(3, concurrentItem.getProductQuantity());
        // new product B must also be present in the fresh cart after recovery
        assertEquals(2, freshCart.getItems().size());
        boolean newProductAdded = freshCart.getItems().stream()
                .anyMatch(item -> item.getProductInfo().getId().equals(newProductId));
        assertTrue(newProductAdded, "Product B should have been added during conflict recovery");
    }

    @Test
    @DisplayName("Add should rethrow DataIntegrityViolationException when it is not a cart-item uniqueness conflict")
    void shouldRethrowUnrelatedDataIntegrityViolation() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");

        ShoppingCart cart = new ShoppingCart();
        cart.setId(UUID.randomUUID());
        cart.setItems(new HashSet<>());

        ProductInfo product = new ProductInfo(
                productId, 1L, "Coffee", "Desc", BigDecimal.valueOf(2.5), 10, true,
                BigDecimal.ZERO, 0, "brand", "seller", "country", 100, 10, 4, 25, 200, 20,
                LocalDateTime.now(), 60, null
        );

        NewShoppingCartItemDto itemToAdd = new NewShoppingCartItemDto();
        itemToAdd.setProductId(productId);
        itemToAdd.setProductQuantity(1);

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(shoppingCartCreator.getOrCreate(userId)).thenReturn(cart);
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(product));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenThrow(new DataIntegrityViolationException("some_other_constraint_violation"));

        org.junit.jupiter.api.Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> addItemsToShoppingCartHelper.add(Set.of(itemToAdd))
        );
    }
}
