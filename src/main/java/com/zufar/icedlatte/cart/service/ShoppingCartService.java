package com.zufar.icedlatte.cart.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.cart.api.CartCheckoutApi;
import com.zufar.icedlatte.cart.api.dto.AddCartItemRequest;
import com.zufar.icedlatte.cart.api.dto.CartSnapshot;
import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.exception.CartProductNotFoundException;
import com.zufar.icedlatte.cart.exception.InvalidCartItemRequestException;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.api.ProductCatalogApi;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService implements CartCheckoutApi {

    private static final int MAX_ITEM_PRODUCT_QUANTITY = 99;

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ProductCatalogApi productCatalogApi;

    @Retryable(retryFor = DataIntegrityViolationException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto getByUserId(final UUID userId) {
        return toCartDto(getOrCreateCart(userId));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public CartSnapshot getByUserIdOrThrow(final UUID userId) {
        return shoppingCartRepository
                .findShoppingCartByUserId(userId)
                .map(this::toCartSnapshot)
                .orElseThrow(() -> new ShoppingCartNotFoundException(userId));
    }

    @Override
    @Retryable(retryFor = DataIntegrityViolationException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public CartSnapshot addItems(final UUID userId, final Set<AddCartItemRequest> itemsToAdd) {
        ShoppingCart shoppingCart = addItemsAndSave(userId, itemsToAdd);
        return toCartSnapshot(shoppingCart);
    }

    @Retryable(retryFor = DataIntegrityViolationException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto addItemsToCart(final UUID userId, final Set<AddCartItemRequest> itemsToAdd) {
        ShoppingCart shoppingCart = addItemsAndSave(userId, itemsToAdd);
        return toCartDto(shoppingCart);
    }

    private ShoppingCart addItemsAndSave(final UUID userId, final Set<AddCartItemRequest> itemsToAdd) {
        validateAddCartItemRequests(itemsToAdd);
        ShoppingCart shoppingCart = getOrCreateCart(userId);
        Map<UUID, Integer> productsWithQuantity = itemsToAdd.stream()
                .collect(Collectors.toMap(
                        AddCartItemRequest::productId, AddCartItemRequest::productQuantity, Integer::sum));
        mergeIntoCart(shoppingCart, productsWithQuantity);
        return shoppingCartRepository.saveAndFlush(shoppingCart);
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto updateItemQuantity(
            final UUID shoppingCartItemId, final UUID userId, final int productQuantityChange) {
        ShoppingCartItem item = shoppingCartItemRepository
                .findByIdAndShoppingCartUserId(shoppingCartItemId, userId)
                .orElseThrow(() -> new ShoppingCartItemNotFoundException(shoppingCartItemId));

        validateQuantityChange(shoppingCartItemId, productQuantityChange, item);

        item.setProductQuantity(item.getProductQuantity() + productQuantityChange);
        shoppingCartItemRepository.save(item);
        return getByUserId(userId);
    }

    @Retryable(retryFor = DataIntegrityViolationException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto deleteItems(final List<UUID> itemIds, final UUID userId) {
        validateDeleteItemIds(itemIds);
        shoppingCartItemRepository.deleteByIdInAndUserId(itemIds, userId);
        log.info("cart.items.deleted: count={}, userId={}", itemIds.size(), userId);
        return getByUserId(userId);
    }

    private ShoppingCart getOrCreateCart(UUID userId) {
        return shoppingCartRepository.findShoppingCartByUserId(userId).orElseGet(() -> createNewShoppingCart(userId));
    }

    private ShoppingCart createNewShoppingCart(UUID userId) {
        ShoppingCart shoppingCart =
                ShoppingCart.builder().userId(userId).items(new HashSet<>()).build();
        ShoppingCart savedCart = shoppingCartRepository.saveAndFlush(shoppingCart);
        log.info("cart.created: userId={}", userId);
        return savedCart;
    }

    private ShoppingCartDto toCartDto(ShoppingCart shoppingCart) {
        Map<UUID, ProductSnapshot> productsById = loadProductsById(shoppingCart);
        return ShoppingCartDtoConverter.toDto(shoppingCart, productsById);
    }

    private CartSnapshot toCartSnapshot(ShoppingCart shoppingCart) {
        Map<UUID, ProductSnapshot> productsById = loadProductsById(shoppingCart);
        return ShoppingCartDtoConverter.toSnapshot(shoppingCart, productsById);
    }

    private Map<UUID, ProductSnapshot> loadProductsById(ShoppingCart shoppingCart) {
        List<UUID> productIds = shoppingCart.getItems().stream()
                .map(ShoppingCartItem::getProductId)
                .toList();
        return productCatalogApi.getProductsByIds(productIds).stream()
                .collect(Collectors.toMap(ProductSnapshot::id, Function.identity()));
    }

    private void mergeIntoCart(ShoppingCart cart, Map<UUID, Integer> productsWithQuantity) {
        increaseExistingItemQuantities(cart, productsWithQuantity);
        cart.getItems().addAll(createNewItems(productsWithQuantity, cart));
    }

    private static void increaseExistingItemQuantities(
            ShoppingCart shoppingCart, Map<UUID, Integer> productsWithQuantity) {
        shoppingCart.getItems().forEach(item -> {
            Integer quantityToAdd = productsWithQuantity.get(item.getProductId());
            if (quantityToAdd != null) {
                int newQuantity = item.getProductQuantity() + quantityToAdd;
                validateProductQuantity(newQuantity);
                item.setProductQuantity(newQuantity);
            }
        });
    }

    private List<ShoppingCartItem> createNewItems(Map<UUID, Integer> productsWithQuantity, ShoppingCart shoppingCart) {
        Set<UUID> existingProductIds = shoppingCart.getItems().stream()
                .map(ShoppingCartItem::getProductId)
                .collect(Collectors.toSet());

        Set<UUID> newProductIds = productsWithQuantity.keySet().stream()
                .filter(productId -> !existingProductIds.contains(productId))
                .collect(Collectors.toSet());

        if (newProductIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> catalogProductIds = productCatalogApi.findExistingProductIds(newProductIds);
        List<UUID> missingIds = newProductIds.stream()
                .filter(productId -> !catalogProductIds.contains(productId))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new CartProductNotFoundException(missingIds);
        }

        return newProductIds.stream()
                .map(productId -> {
                    int productQuantity = Objects.requireNonNull(
                            productsWithQuantity.get(productId),
                            "Product quantity not found for productId: " + productId);
                    validateProductQuantity(productQuantity);
                    return ShoppingCartItem.builder()
                            .shoppingCart(shoppingCart)
                            .productId(productId)
                            .productQuantity(productQuantity)
                            .build();
                })
                .toList();
    }

    private void validateQuantityChange(
            final UUID shoppingCartItemId, int productQuantityChange, ShoppingCartItem item) {
        if (productQuantityChange == 0) {
            log.debug("cart.item.quantity.zero_change: itemId={}", shoppingCartItemId);
            throw new InvalidItemProductQuantityException("Product quantity change must not be zero.");
        }
        int newQuantity = item.getProductQuantity() + productQuantityChange;
        validateProductQuantity(newQuantity);
    }

    private static void validateProductQuantity(int productQuantity) {
        if (productQuantity < 1 || productQuantity > MAX_ITEM_PRODUCT_QUANTITY) {
            throw new InvalidItemProductQuantityException(productQuantity, MAX_ITEM_PRODUCT_QUANTITY);
        }
    }

    private static void validateAddCartItemRequests(Set<AddCartItemRequest> itemsToAdd) {
        if (itemsToAdd.isEmpty()) {
            throw new InvalidCartItemRequestException("Cart items to add must not be empty.");
        }
        for (AddCartItemRequest item : itemsToAdd) {
            validateProductQuantity(item.productQuantity());
        }
    }

    private static void validateDeleteItemIds(List<UUID> itemIds) {
        if (itemIds.isEmpty()) {
            throw new InvalidCartItemRequestException("Cart item ids to delete must not be empty.");
        }
    }

    @Override
    @Transactional
    public void deleteCartForUser(final UUID userId) {
        shoppingCartRepository.deleteByUserId(userId);
    }
}
