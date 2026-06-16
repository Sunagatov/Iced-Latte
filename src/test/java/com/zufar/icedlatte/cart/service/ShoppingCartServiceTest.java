package com.zufar.icedlatte.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.exception.CartProductNotFoundException;
import com.zufar.icedlatte.cart.exception.InvalidCartItemRequestException;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingCartService unit tests")
class ShoppingCartServiceTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;

    @Mock
    private ShoppingCartItemRepository shoppingCartItemRepository;

    @Mock
    private ProductCatalogApi productCatalogApi;

    private ShoppingCartService shoppingCartService;

    @BeforeEach
    void setUp() {
        shoppingCartService =
                new ShoppingCartService(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("getByUserId returns the existing cart dto")
    void getByUserIdReturnsExistingCartDto() {
        UUID userId = UUID.randomUUID();
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(productCatalogApi.getProductsByIds(any()))
                .thenReturn(
                        CartDtoTestStub.createProductsById().values().stream().toList());

        ShoppingCartDto result = shoppingCartService.getByUserId(userId);

        assertThat(result.getId()).isEqualTo(shoppingCart.getId());
        assertThat(result.getItems()).hasSize(3);
        assertThat(result.getProductsQuantity()).isEqualTo(6);
        assertThat(result.getItems())
                .allSatisfy(item ->
                        assertThat(item.getProductInfo().getDescription()).isNotBlank());
        assertThat(result.getItems()).allSatisfy(item -> {
            assertThat(item.getProductInfo().getAverageRating()).isNotNull();
            assertThat(item.getProductInfo().getReviewsCount()).isNotNull();
            assertThat(item.getProductInfo().getBrandName()).isEqualTo("Test Brand");
            assertThat(item.getProductInfo().getSellerName()).isEqualTo("Test Seller");
            assertThat(item.getProductInfo().getWeight()).isNotNull();
        });
    }

    @Test
    @DisplayName("getByUserId creates and returns a new cart when one does not exist")
    void getByUserIdCreatesCartWhenMissing() {
        UUID userId = UUID.randomUUID();
        ShoppingCart savedCart = ShoppingCart.builder()
                .userId(userId)
                .items(new HashSet<>())
                .createdAt(OffsetDateTime.now())
                .build();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.empty());
        when(shoppingCartRepository.saveAndFlush(any(ShoppingCart.class))).thenReturn(savedCart);

        ShoppingCartDto result = shoppingCartService.getByUserId(userId);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getItems()).isEmpty();
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getItems()).isEmpty();
    }

    @Test
    @DisplayName("getByUserId does not call product catalog for an empty cart")
    void getByUserIdDoesNotCallProductCatalogForEmptyCart() {
        UUID userId = UUID.randomUUID();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .items(new HashSet<>())
                .createdAt(OffsetDateTime.now())
                .build();
        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));

        ShoppingCartDto result = shoppingCartService.getByUserId(userId);

        assertThat(result.getItems()).isEmpty();
        verifyNoInteractions(productCatalogApi);
    }

    @Test
    @DisplayName("getByUserId fails with product not found when a cart item no longer exists in the catalog")
    void getByUserIdFailsWhenCartContainsRemovedProduct() {
        UUID userId = UUID.randomUUID();
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(productCatalogApi.getProductsByIds(any()))
                .thenReturn(List.of(
                        CartDtoTestStub.createProductsById().get(CartDtoTestStub.SECOND_PRODUCT_ID),
                        CartDtoTestStub.createProductsById().get(CartDtoTestStub.THIRD_PRODUCT_ID)));

        assertThatThrownBy(() -> shoppingCartService.getByUserId(userId))
                .isInstanceOf(CartProductNotFoundException.class)
                .hasMessageContaining(CartDtoTestStub.FIRST_PRODUCT_ID.toString());
    }

    @Test
    @DisplayName("getByUserIdOrThrow fails when the user has no cart")
    void getByUserIdOrThrowFailsWhenCartMissing() {
        UUID userId = UUID.randomUUID();
        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingCartService.getByUserIdOrThrow(userId))
                .isInstanceOf(ShoppingCartNotFoundException.class);
    }

    @Test
    @DisplayName("addItems increases existing quantities and adds new products")
    void addItemsMergesExistingAndNewProducts() {
        UUID userId = UUID.randomUUID();
        UUID existingProductId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        UUID newProductId = UUID.fromString("b58ac6f1-7ee1-4888-9055-3bebb6aa3632");

        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setId(UUID.randomUUID());
        shoppingCart.setUserId(userId);
        ShoppingCartItem existingItem = ShoppingCartItem.builder()
                .id(UUID.randomUUID())
                .shoppingCart(shoppingCart)
                .productId(existingProductId)
                .productQuantity(1)
                .build();
        shoppingCart.setItems(new HashSet<>(Set.of(existingItem)));

        AddCartItemRequest existingProductToAdd = new AddCartItemRequest(existingProductId, 2);
        AddCartItemRequest newProductToAdd = new AddCartItemRequest(newProductId, 3);

        ProductSnapshot existingProduct = productDto(existingProductId, BigDecimal.valueOf(1.5));
        ProductSnapshot newProduct = productDto(newProductId, BigDecimal.valueOf(3.5));

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(productCatalogApi.findExistingProductIds(Set.of(existingProductId, newProductId)))
                .thenReturn(Set.of(existingProductId, newProductId));
        when(productCatalogApi.getProductsByIds(any())).thenReturn(List.of(existingProduct, newProduct));
        when(shoppingCartRepository.saveAndFlush(shoppingCart)).thenReturn(shoppingCart);

        ShoppingCartDto result =
                shoppingCartService.addItemsToCart(userId, Set.of(existingProductToAdd, newProductToAdd));

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getProductsQuantity()).isEqualTo(6);
        assertThat(existingItem.getProductQuantity()).isEqualTo(3);
        assertThat(shoppingCart.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("addItems throws when a requested product does not exist")
    void addItemsFailsWhenProductMissing() {
        UUID userId = UUID.randomUUID();
        UUID missingProductId = UUID.randomUUID();
        ShoppingCart cart = new ShoppingCart();
        cart.setId(UUID.randomUUID());
        cart.setItems(new HashSet<>());

        AddCartItemRequest itemToAdd = new AddCartItemRequest(missingProductId, 1);

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogApi.findExistingProductIds(Set.of(missingProductId))).thenReturn(Set.of());

        assertThatThrownBy(() -> shoppingCartService.addItemsToCart(userId, Set.of(itemToAdd)))
                .isInstanceOf(CartProductNotFoundException.class);
    }

    @Test
    @DisplayName("addItems rejects quantity increase for a cart item whose product was removed from the catalog")
    void addItemsRejectsIncreaseForRemovedExistingProduct() {
        UUID userId = UUID.randomUUID();
        UUID removedProductId = UUID.randomUUID();
        ShoppingCart cart = new ShoppingCart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        ShoppingCartItem existingItem = ShoppingCartItem.builder()
                .id(UUID.randomUUID())
                .shoppingCart(cart)
                .productId(removedProductId)
                .productQuantity(1)
                .build();
        cart.setItems(new HashSet<>(Set.of(existingItem)));

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogApi.findExistingProductIds(Set.of(removedProductId))).thenReturn(Set.of());

        assertThatThrownBy(() ->
                        shoppingCartService.addItemsToCart(userId, Set.of(new AddCartItemRequest(removedProductId, 1))))
                .isInstanceOf(CartProductNotFoundException.class)
                .hasMessageContaining(removedProductId.toString());

        verify(shoppingCartRepository, never()).saveAndFlush(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("addItems rejects empty internal requests")
    void addItemsRejectsEmptyInternalRequests() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> shoppingCartService.addItems(userId, Set.of()))
                .isInstanceOf(InvalidCartItemRequestException.class);

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("addItems rejects invalid internal item quantities")
    void addItemsRejectsInvalidInternalItemQuantities() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() ->
                        shoppingCartService.addItems(userId, Set.of(new AddCartItemRequest(UUID.randomUUID(), 100))))
                .isInstanceOf(InvalidItemProductQuantityException.class);

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("addItems rejects null internal requests")
    @SuppressWarnings("DataFlowIssue")
    void addItemsRejectsNullInternalRequests() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> shoppingCartService.addItems(userId, null))
                .isInstanceOf(InvalidCartItemRequestException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("addItems rejects null items inside internal requests")
    void addItemsRejectsNullItemsInsideInternalRequests() {
        UUID userId = UUID.randomUUID();
        Set<AddCartItemRequest> itemsToAdd = new HashSet<>();
        itemsToAdd.add(null);

        assertThatThrownBy(() -> shoppingCartService.addItems(userId, itemsToAdd))
                .isInstanceOf(InvalidCartItemRequestException.class)
                .hasMessageContaining("must not contain null items");

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("addItems rejects new item quantities above the cart item limit")
    void addItemsRejectsNewItemQuantityAboveLimit() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        AddCartItemRequest itemToAdd = new AddCartItemRequest(productId, 100);

        assertThatThrownBy(() -> shoppingCartService.addItemsToCart(userId, Set.of(itemToAdd)))
                .isInstanceOf(InvalidItemProductQuantityException.class);

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("addItems rejects merged quantities above the cart item limit")
    void addItemsRejectsMergedQuantityAboveLimit() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setId(UUID.randomUUID());
        ShoppingCartItem existingItem = ShoppingCartItem.builder()
                .id(UUID.randomUUID())
                .shoppingCart(shoppingCart)
                .productId(productId)
                .productQuantity(98)
                .build();
        shoppingCart.setItems(new HashSet<>(Set.of(existingItem)));

        AddCartItemRequest itemToAdd = new AddCartItemRequest(productId, 2);

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(productCatalogApi.findExistingProductIds(Set.of(productId))).thenReturn(Set.of(productId));

        assertThatThrownBy(() -> shoppingCartService.addItemsToCart(userId, Set.of(itemToAdd)))
                .isInstanceOf(InvalidItemProductQuantityException.class);

        verify(shoppingCartRepository, never()).saveAndFlush(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("updateItemQuantity returns the refreshed cart for a valid change")
    void updateItemQuantityReturnsRefreshedCart() {
        int productQuantityChange = 5;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID userId = UUID.randomUUID();

        ShoppingCart cart = shoppingCartItem.getShoppingCart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setItems(new HashSet<>(Set.of(shoppingCartItem)));

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userId))
                .thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(shoppingCartItem);
        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogApi.getProductsByIds(any()))
                .thenReturn(List.of(productDto(shoppingCartItem.getProductId(), BigDecimal.valueOf(2.5))));

        ShoppingCartDto result =
                shoppingCartService.updateItemQuantity(shoppingCartItem.getId(), userId, productQuantityChange);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getProductQuantity()).isEqualTo(10);
        verify(shoppingCartItemRepository).save(shoppingCartItem);
    }

    @Test
    @DisplayName("updateItemQuantity rejects invalid quantity changes")
    void updateItemQuantityRejectsInvalidChanges() {
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userId))
                .thenReturn(Optional.of(shoppingCartItem));

        assertThatThrownBy(() -> shoppingCartService.updateItemQuantity(shoppingCartItem.getId(), userId, 0))
                .isInstanceOf(InvalidItemProductQuantityException.class);

        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
        verifyNoInteractions(shoppingCartRepository);
    }

    @Test
    @DisplayName("updateItemQuantity rejects resulting quantities above the cart item limit")
    void updateItemQuantityRejectsQuantityAboveLimit() {
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userId))
                .thenReturn(Optional.of(shoppingCartItem));

        assertThatThrownBy(() -> shoppingCartService.updateItemQuantity(shoppingCartItem.getId(), userId, 95))
                .isInstanceOf(InvalidItemProductQuantityException.class);

        verify(shoppingCartItemRepository, never()).save(any(ShoppingCartItem.class));
        verifyNoInteractions(shoppingCartRepository);
    }

    @Test
    @DisplayName("updateItemQuantity returns not found for foreign or missing items")
    void updateItemQuantityReturnsNotFoundForMissingItems() {
        UUID itemId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(itemId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> shoppingCartService.updateItemQuantity(itemId, userId, 1))
                .isInstanceOf(ShoppingCartItemNotFoundException.class);
    }

    @Test
    @DisplayName("deleteItems removes the requested items and returns the updated cart")
    void deleteItemsRemovesItemsAndReturnsCart() {
        UUID userId = UUID.randomUUID();
        List<UUID> itemIdsForDelete =
                Collections.singletonList(UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(productCatalogApi.getProductsByIds(any()))
                .thenReturn(
                        CartDtoTestStub.createProductsById().values().stream().toList());

        ShoppingCartDto actualResult = shoppingCartService.deleteItems(itemIdsForDelete, userId);

        assertThat(actualResult.getItems()).hasSize(3);
        verify(shoppingCartItemRepository).deleteByIdInAndUserId(itemIdsForDelete, userId);
    }

    @Test
    @DisplayName("deleteItems rejects empty item ids before touching repositories")
    void deleteItemsRejectsEmptyItemIds() {
        UUID userId = UUID.randomUUID();
        assertThatThrownBy(() -> shoppingCartService.deleteItems(List.of(), userId))
                .isInstanceOf(InvalidCartItemRequestException.class);

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("deleteItems rejects null item ids before touching repositories")
    @SuppressWarnings("DataFlowIssue")
    void deleteItemsRejectsNullItemIds() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(
                        () -> shoppingCartService.deleteItems(new ArrayList<>(Collections.singletonList(null)), userId))
                .isInstanceOf(InvalidCartItemRequestException.class);

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    @Test
    @DisplayName("deleteItems rejects null item id collections before touching repositories")
    @SuppressWarnings("DataFlowIssue")
    void deleteItemsRejectsNullItemIdCollections() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> shoppingCartService.deleteItems(null, userId))
                .isInstanceOf(InvalidCartItemRequestException.class)
                .hasMessageContaining("must not be null");

        verifyNoInteractions(shoppingCartRepository, shoppingCartItemRepository, productCatalogApi);
    }

    private static ProductSnapshot productDto(UUID id, BigDecimal price) {
        return new ProductSnapshot(
                id, "New coffee", "Desc", price, 10, true, null, BigDecimal.valueOf(4.5), 12, "Brand", "Seller", 250);
    }
}
