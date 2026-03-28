package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        increaseExistingItemQuantities(shoppingCart, productsWithQuantity);
        List<ShoppingCartItem> newItems = createNewItems(productsWithQuantity, shoppingCart);

        ShoppingCart updatedShoppingCart = updateExistingShoppingCart(shoppingCart, newItems, productsWithQuantity);

        ShoppingCart persistedShoppingCart = shoppingCartRepository.save(updatedShoppingCart);
        return shoppingCartDtoConverter.toDto(persistedShoppingCart);
    }

    private static Map<UUID, Integer> extractProductsWithQuantity(Set<NewShoppingCartItemDto> itemsToAdd) {
        return itemsToAdd.stream()
                .collect(Collectors.toMap(
                        NewShoppingCartItemDto::getProductId,
                        NewShoppingCartItemDto::getProductQuantity,
                        Integer::sum
                ));
    }

    private static void increaseExistingItemQuantities(ShoppingCart shoppingCart, Map<UUID, Integer> productsWithQuantity) {
        shoppingCart.getItems().forEach(item -> {
            UUID productId = item.getProductInfo().getId();
            Integer quantityToAdd = productsWithQuantity.get(productId);

            if (quantityToAdd != null) {
                item.setProductQuantity(item.getProductQuantity() + quantityToAdd);
            }
        });
    }

    private List<ShoppingCartItem> createNewItems(Map<UUID, Integer> productsWithQuantity, ShoppingCart shoppingCart) {
        Map<UUID, ShoppingCartItem> existingItemsByProductId = shoppingCart.getItems().stream()
                .collect(Collectors.toMap(item -> item.getProductInfo().getId(), Function.identity()));

        Set<UUID> newProductIds = productsWithQuantity.keySet().stream()
                .filter(productId -> !existingItemsByProductId.containsKey(productId))
                .collect(Collectors.toSet());

        return productInfoRepository.findAllById(newProductIds).stream()
                .map(productInfo ->
                        ShoppingCartItem.builder()
                                .shoppingCart(shoppingCart)
                                .productQuantity(productsWithQuantity.get(productInfo.getId()))
                                .productInfo(productInfo)
                                .build()
                )
                .toList();
    }

    private static ShoppingCart updateExistingShoppingCart(ShoppingCart existingShoppingCart,
                                                           List<ShoppingCartItem> shoppingCartItems,
                                                           Map<UUID, Integer> productsWithQuantity) {
        int addedProductsQuantity = productsWithQuantity.values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        existingShoppingCart.setItemsQuantity(existingShoppingCart.getItemsQuantity() + shoppingCartItems.size());
        existingShoppingCart.setProductsQuantity(existingShoppingCart.getProductsQuantity() + addedProductsQuantity);
        existingShoppingCart.getItems().addAll(shoppingCartItems);
        return existingShoppingCart;
    }
}
