package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.DeleteItemsFromShoppingCartRequest;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartItemDto;
import com.zufar.icedlatte.product.api.filestorage.ProductPictureLinkUpdater;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private static final int MAX_ITEM_PRODUCT_QUANTITY = 99;

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ProductInfoRepository productInfoRepository;
    private final ShoppingCartDtoConverter shoppingCartDtoConverter;
    private final ProductPictureLinkUpdater productPictureLinkUpdater;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto getByUserId(final UUID userId) {
        return toCartDto(getOrCreateCart(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public ShoppingCartDto getByUserIdOrThrow(final UUID userId) {
        return shoppingCartRepository.findShoppingCartByUserId(userId)
                .map(this::toCartDto)
                .orElseThrow(() -> new ShoppingCartNotFoundException(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto addItems(final UUID userId,
                                    final Set<NewShoppingCartItemDto> itemsToAdd) {
        ShoppingCart shoppingCart = getOrCreateCart(userId);
        Map<UUID, Integer> productsWithQuantity = extractProductsWithQuantity(itemsToAdd);
        mergeIntoCart(shoppingCart, productsWithQuantity);

        try {
            return toCartDto(shoppingCartRepository.save(shoppingCart));
        } catch (DataIntegrityViolationException ex) {
            if (!isCartItemUniqueConstraintViolation(ex)) {
                throw ex;
            }
            log.warn("cart.items.add.concurrent_conflict: userId={}", userId);
            ShoppingCart freshCart = getOrCreateCart(userId);
            mergeIntoCart(freshCart, productsWithQuantity);
            return toCartDto(shoppingCartRepository.save(freshCart));
        }
    }

    @Retryable(retryFor = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto updateItemQuantity(final UUID shoppingCartItemId,
                                              final UUID userId,
                                              final int productQuantityChange) {
        ShoppingCartItem item = shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItemId, userId)
                .orElseThrow(() -> new ShoppingCartItemNotFoundException(shoppingCartItemId));

        validateQuantityChange(shoppingCartItemId, productQuantityChange, item);

        item.setProductQuantity(item.getProductQuantity() + productQuantityChange);
        shoppingCartItemRepository.save(item);
        return getByUserId(userId);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto deleteItems(final DeleteItemsFromShoppingCartRequest request,
                                       final UUID userId) {
        List<UUID> itemIds = request.getShoppingCartItemIds();
        shoppingCartItemRepository.deleteByIdInAndUserId(itemIds, userId);
        log.info("cart.items.deleted: count={}, userId={}", itemIds.size(), userId);
        return getByUserId(userId);
    }

    private ShoppingCart getOrCreateCart(UUID userId) {
        return shoppingCartRepository.findShoppingCartByUserId(userId)
                .orElseGet(() -> createNewShoppingCart(userId));
    }

    private ShoppingCart createNewShoppingCart(UUID userId) {
        try {
            ShoppingCart shoppingCart = ShoppingCart.builder()
                    .userId(userId)
                    .items(new HashSet<>())
                    .build();
            shoppingCartRepository.save(shoppingCart);
            log.info("cart.created: userId={}", userId);
            return shoppingCart;
        } catch (DataIntegrityViolationException ex) {
            log.warn("cart.create.concurrent_conflict: userId={}", userId);
            return shoppingCartRepository.findShoppingCartByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Cart not found after uniqueness conflict for userId=" + userId, ex));
        }
    }

    private ShoppingCartDto toCartDto(ShoppingCart shoppingCart) {
        ShoppingCartDto cart = shoppingCartDtoConverter.toDto(shoppingCart);
        if (cart.getItems() != null) {
            productPictureLinkUpdater.updateBatch(
                    cart.getItems().stream().map(ShoppingCartItemDto::getProductInfo).toList());
        }
        return cart;
    }

    private void mergeIntoCart(ShoppingCart cart, Map<UUID, Integer> productsWithQuantity) {
        increaseExistingItemQuantities(cart, productsWithQuantity);
        cart.getItems().addAll(createNewItems(productsWithQuantity, cart));
    }

    private static boolean isCartItemUniqueConstraintViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        return msg != null && msg.contains("uq_shopping_cart_item_cart_product");
    }

    private static Map<UUID, Integer> extractProductsWithQuantity(Set<NewShoppingCartItemDto> itemsToAdd) {
        return itemsToAdd.stream()
                .collect(Collectors.toMap(
                        NewShoppingCartItemDto::getProductId,
                        NewShoppingCartItemDto::getProductQuantity,
                        Integer::sum
                ));
    }

    private static void increaseExistingItemQuantities(ShoppingCart shoppingCart,
                                                       Map<UUID, Integer> productsWithQuantity) {
        shoppingCart.getItems().forEach(item -> {
            UUID productId = item.getProductInfo().getId();
            Integer quantityToAdd = productsWithQuantity.get(productId);

            if (quantityToAdd != null) {
                int newQuantity = item.getProductQuantity() + quantityToAdd;
                validateProductQuantity(newQuantity);
                item.setProductQuantity(newQuantity);
            }
        });
    }

    private List<ShoppingCartItem> createNewItems(Map<UUID, Integer> productsWithQuantity,
                                                  ShoppingCart shoppingCart) {
        Map<UUID, ShoppingCartItem> existingItemsByProductId = shoppingCart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductInfo().getId(), Function.identity()));

        Set<UUID> newProductIds = productsWithQuantity.keySet().stream()
                .filter(productId -> !existingItemsByProductId.containsKey(productId))
                .collect(Collectors.toSet());

        List<ProductInfo> foundProducts = productInfoRepository.findAllById(newProductIds);
        Set<UUID> foundIds = foundProducts.stream()
                .map(ProductInfo::getId)
                .collect(Collectors.toSet());
        List<UUID> missingIds = newProductIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ProductNotFoundException(missingIds);
        }

        return foundProducts.stream()
                .map(productInfo -> {
                    Integer productQuantity = productsWithQuantity.get(productInfo.getId());
                    validateProductQuantity(productQuantity);
                    return ShoppingCartItem.builder()
                            .shoppingCart(shoppingCart)
                            .productQuantity(productQuantity)
                            .productInfo(productInfo)
                            .build();
                })
                .toList();
    }

    private void validateQuantityChange(final UUID shoppingCartItemId,
                                        int productQuantityChange,
                                        ShoppingCartItem item) {
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
}
