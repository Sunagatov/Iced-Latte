package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.exception.ProductNotFoundException;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddItemsToShoppingCartHelper {

    private final ShoppingCartRepository shoppingCartRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductInfoRepository productInfoRepository;
    private final ShoppingCartDtoConverter shoppingCartDtoConverter;
    private final ShoppingCartCreator shoppingCartCreator;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto add(final Set<NewShoppingCartItemDto> itemsToAdd) {
        UUID userId = securityPrincipalProvider.getUserId();

        ShoppingCart shoppingCart = shoppingCartCreator.getOrCreate(userId);
        Map<UUID, Integer> productsWithQuantity = extractProductsWithQuantity(itemsToAdd);
        mergeIntoCart(shoppingCart, productsWithQuantity);

        try {
            ShoppingCart persistedShoppingCart = shoppingCartRepository.save(shoppingCart);
            return shoppingCartDtoConverter.toDto(persistedShoppingCart);
        } catch (DataIntegrityViolationException ex) {
            if (!isCartItemUniqueConstraintViolation(ex)) {
                throw ex;
            }
            // A concurrent request inserted the same product between our read and save.
            // Re-read the cart (which now contains the concurrent item) and do a full merge.
            log.warn("cart.items.add.concurrent_conflict: userId={}", userId);
            ShoppingCart freshCart = shoppingCartCreator.getOrCreate(userId);
            mergeIntoCart(freshCart, productsWithQuantity);
            ShoppingCart persistedShoppingCart = shoppingCartRepository.save(freshCart);
            return shoppingCartDtoConverter.toDto(persistedShoppingCart);
        }
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
                item.setProductQuantity(item.getProductQuantity() + quantityToAdd);
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
                .map(productInfo ->
                        ShoppingCartItem.builder()
                                .shoppingCart(shoppingCart)
                                .productQuantity(productsWithQuantity.get(productInfo.getId()))
                                .productInfo(productInfo)
                                .build()
                )
                .toList();
    }

}
