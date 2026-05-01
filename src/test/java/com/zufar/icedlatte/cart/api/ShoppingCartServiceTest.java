package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingCartService unit tests")
class ShoppingCartServiceTest {

    @Mock private ShoppingCartRepository shoppingCartRepository;
    @Mock private ShoppingCartItemRepository shoppingCartItemRepository;
    @Mock private ProductInfoRepository productInfoRepository;
    @Mock private ShoppingCartDtoConverter shoppingCartDtoConverter;
    @Mock private ProductPictureLinkUpdater productPictureLinkUpdater;

    @InjectMocks private ShoppingCartService shoppingCartService;

    @Test
    @DisplayName("getByUserId returns the existing cart dto")
    void getByUserIdReturnsExistingCartDto() {
        UUID userId = UUID.randomUUID();
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();
        ShoppingCartDto expectedDto = CartDtoTestStub.createShoppingCartDto();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(shoppingCartDtoConverter.toDto(shoppingCart)).thenReturn(expectedDto);

        ShoppingCartDto result = shoppingCartService.getByUserId(userId);

        assertThat(result).isEqualTo(expectedDto);
        verify(productPictureLinkUpdater).updateBatch(any());
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
        ShoppingCartDto expectedDto = new ShoppingCartDto();
        expectedDto.setItems(List.of());

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.empty());
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(savedCart);
        when(shoppingCartDtoConverter.toDto(savedCart)).thenReturn(expectedDto);

        ShoppingCartDto result = shoppingCartService.getByUserId(userId);

        assertThat(result).isEqualTo(expectedDto);
        ArgumentCaptor<ShoppingCart> captor = ArgumentCaptor.forClass(ShoppingCart.class);
        verify(shoppingCartRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getItems()).isEmpty();
    }

    @Test
    @DisplayName("getByUserId reuses the winning cart after a concurrent create conflict")
    void getByUserIdReusesCartAfterConcurrentCreateConflict() {
        UUID userId = UUID.randomUUID();
        ShoppingCart existingCart = ShoppingCart.builder().userId(userId).items(new HashSet<>()).build();
        ShoppingCartDto expectedDto = new ShoppingCartDto();
        expectedDto.setItems(List.of());

        when(shoppingCartRepository.findShoppingCartByUserId(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingCart));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(shoppingCartDtoConverter.toDto(existingCart)).thenReturn(expectedDto);

        ShoppingCartDto result = shoppingCartService.getByUserId(userId);

        assertThat(result).isEqualTo(expectedDto);
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

        ProductInfo existingProduct = product(existingProductId, "Existing coffee", BigDecimal.valueOf(2.5));
        ShoppingCartItem existingItem = new ShoppingCartItem(UUID.randomUUID(), shoppingCart, existingProduct, 1);
        shoppingCart.setItems(new HashSet<>(Set.of(existingItem)));

        NewShoppingCartItemDto existingProductToAdd = new NewShoppingCartItemDto();
        existingProductToAdd.setProductId(existingProductId);
        existingProductToAdd.setProductQuantity(2);

        NewShoppingCartItemDto newProductToAdd = new NewShoppingCartItemDto();
        newProductToAdd.setProductId(newProductId);
        newProductToAdd.setProductQuantity(3);

        ProductInfo newProduct = product(newProductId, "New coffee", BigDecimal.valueOf(3.5));
        ShoppingCartDto expectedDto = new ShoppingCartDto();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(productInfoRepository.findAllById(Set.of(newProductId))).thenReturn(List.of(newProduct));
        when(shoppingCartRepository.save(shoppingCart)).thenReturn(shoppingCart);
        when(shoppingCartDtoConverter.toDto(shoppingCart)).thenReturn(expectedDto);

        ShoppingCartDto result = shoppingCartService.addItems(userId, Set.of(existingProductToAdd, newProductToAdd));

        assertThat(result).isEqualTo(expectedDto);
        assertThat(existingItem.getProductQuantity()).isEqualTo(3);
        assertThat(shoppingCart.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("addItems recovers when concurrent insert causes a uniqueness conflict")
    void addItemsRecoversAfterConcurrentConflict() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");

        ShoppingCart firstCart = new ShoppingCart();
        firstCart.setId(UUID.randomUUID());
        firstCart.setItems(new HashSet<>());

        ProductInfo product = product(productId, "Coffee", BigDecimal.valueOf(2.5));
        ShoppingCart freshCart = new ShoppingCart();
        freshCart.setId(firstCart.getId());
        ShoppingCartItem concurrentItem = new ShoppingCartItem(UUID.randomUUID(), freshCart, product, 1);
        freshCart.setItems(new HashSet<>(Set.of(concurrentItem)));

        NewShoppingCartItemDto itemToAdd = new NewShoppingCartItemDto();
        itemToAdd.setProductId(productId);
        itemToAdd.setProductQuantity(2);

        ShoppingCartDto expectedDto = new ShoppingCartDto();

        when(shoppingCartRepository.findShoppingCartByUserId(userId))
                .thenReturn(Optional.of(firstCart))
                .thenReturn(Optional.of(freshCart));
        when(productInfoRepository.findAllById(any())).thenReturn(List.of(product));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenThrow(new DataIntegrityViolationException("uq_shopping_cart_item_cart_product"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingCartDtoConverter.toDto(freshCart)).thenReturn(expectedDto);

        ShoppingCartDto result = shoppingCartService.addItems(userId, Set.of(itemToAdd));

        assertThat(result).isEqualTo(expectedDto);
        assertThat(concurrentItem.getProductQuantity()).isEqualTo(3);
        verify(shoppingCartRepository, times(2)).save(any(ShoppingCart.class));
    }

    @Test
    @DisplayName("addItems throws when a requested product does not exist")
    void addItemsFailsWhenProductMissing() {
        UUID userId = UUID.randomUUID();
        UUID missingProductId = UUID.randomUUID();
        ShoppingCart cart = new ShoppingCart();
        cart.setId(UUID.randomUUID());
        cart.setItems(new HashSet<>());

        NewShoppingCartItemDto itemToAdd = new NewShoppingCartItemDto();
        itemToAdd.setProductId(missingProductId);
        itemToAdd.setProductQuantity(1);

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(cart));
        when(productInfoRepository.findAllById(Set.of(missingProductId))).thenReturn(List.of());

        assertThatThrownBy(() -> shoppingCartService.addItems(userId, Set.of(itemToAdd)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("updateItemQuantity returns the refreshed cart for a valid change")
    void updateItemQuantityReturnsRefreshedCart() {
        int productQuantityChange = 5;
        ShoppingCartItem shoppingCartItem = CartDtoTestStub.createShoppingCartItem();
        UUID userId = UUID.randomUUID();
        ShoppingCartDto cartDto = CartDtoTestStub.createShoppingCartDto();

        when(shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItem.getId(), userId))
                .thenReturn(Optional.of(shoppingCartItem));
        when(shoppingCartItemRepository.save(shoppingCartItem)).thenReturn(shoppingCartItem);
        when(shoppingCartRepository.findShoppingCartByUserId(userId))
                .thenReturn(Optional.of(shoppingCartItem.getShoppingCart()));
        when(shoppingCartDtoConverter.toDto(shoppingCartItem.getShoppingCart())).thenReturn(cartDto);

        ShoppingCartDto result = shoppingCartService.updateItemQuantity(shoppingCartItem.getId(), userId, productQuantityChange);

        assertThat(result).isEqualTo(cartDto);
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
        verifyNoInteractions(shoppingCartRepository, shoppingCartDtoConverter);
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
        List<UUID> itemIdsForDelete = Collections.singletonList(UUID.fromString("b00ed4dc-62d1-449c-b559-65d9c2cad906"));
        DeleteItemsFromShoppingCartRequest request = new DeleteItemsFromShoppingCartRequest();
        request.shoppingCartItemIds(itemIdsForDelete);
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();
        ShoppingCartDto expectedResult = CartDtoTestStub.createShoppingCartDto();

        when(shoppingCartRepository.findShoppingCartByUserId(userId)).thenReturn(Optional.of(shoppingCart));
        when(shoppingCartDtoConverter.toDto(shoppingCart)).thenReturn(expectedResult);

        ShoppingCartDto actualResult = shoppingCartService.deleteItems(request, userId);

        assertThat(actualResult).isEqualTo(expectedResult);
        verify(shoppingCartItemRepository).deleteByIdInAndUserId(itemIdsForDelete, userId);
    }

    private static ProductInfo product(UUID id, String name, BigDecimal price) {
        return new ProductInfo(
                id, 1L, name, "Desc", price, 10, true,
                BigDecimal.ZERO, 0, "brand", "seller", "country", 100, 10, 4, 25, 200, 20,
                LocalDateTime.now(), 60, null
        );
    }
}
