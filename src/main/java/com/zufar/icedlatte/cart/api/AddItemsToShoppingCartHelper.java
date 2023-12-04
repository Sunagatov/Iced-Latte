package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingCartDtoConverter;
import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.NewShoppingCartItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import com.zufar.icedlatte.product.entity.ProductInfo;
import com.zufar.icedlatte.product.repository.ProductInfoRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddItemsToShoppingCartHelper {

    public static final int DEFAULT_PRODUCTS_QUANTITY = 0;
    public static final int DEFAULT_ITEMS_QUANTITY = 0;

    private final ShoppingCartRepository shoppingCartRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductInfoRepository productInfoRepository;
    private final ShoppingCartDtoConverter shoppingCartDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto add(final Set<NewShoppingCartItemDto> itemsToAdd) {
        UUID userId = securityPrincipalProvider.getUserId();

        ShoppingCart shoppingCart = Optional.ofNullable(shoppingCartRepository.findShoppingCartByUserId(userId))
                .orElseGet(() -> createNewShoppingCart(userId));

        List<ShoppingCartItem> items = createItems(itemsToAdd, shoppingCart);

        ShoppingCart updatedShoppingCart = updateExistingShoppingCart(shoppingCart, items);

        ShoppingCart persistedShoppingCart = shoppingCartRepository.save(updatedShoppingCart);
        return shoppingCartDtoConverter.toDto(persistedShoppingCart);
    }

    private List<ShoppingCartItem> createItems(Set<NewShoppingCartItemDto> itemsToAdd, ShoppingCart shoppingCart) {
        Map<UUID, Integer> productsWithQuantity = itemsToAdd.stream()
                .collect(Collectors.toMap(NewShoppingCartItemDto::getProductId, NewShoppingCartItemDto::getProductQuantity));

        Set<UUID> existedProductIds = shoppingCart.getItems().stream()
                .map(ShoppingCartItem::getProductInfo)
                .map(ProductInfo::getProductId)
                .collect(Collectors.toSet());

        Set<UUID> newProductIds = productsWithQuantity.keySet().stream()
                .filter(productId -> !existedProductIds.contains(productId))
                .collect(Collectors.toSet());

        return productInfoRepository.findAllById(newProductIds).stream()
                .map(productInfo ->
                        ShoppingCartItem.builder()
                                .shoppingCart(shoppingCart)
                                .productQuantity(productsWithQuantity.get(productInfo.getProductId()))
                                .productInfo(productInfo)
                                .build()
                )
                .toList();
    }

    private static ShoppingCart updateExistingShoppingCart(ShoppingCart existingShoppingCart,
                                                           List<ShoppingCartItem> shoppingCartItems) {
        int productsQuantity = shoppingCartItems.stream()
                .map(ShoppingCartItem::getProductQuantity)
                .reduce(Integer::sum)
                .orElse(DEFAULT_PRODUCTS_QUANTITY);

        existingShoppingCart.setItemsQuantity(existingShoppingCart.getItemsQuantity() + shoppingCartItems.size());
        existingShoppingCart.setProductsQuantity(existingShoppingCart.getProductsQuantity() + productsQuantity);
        existingShoppingCart.getItems().addAll(shoppingCartItems);
        return existingShoppingCart;
    }

    private static ShoppingCart createNewShoppingCart(UUID userId) {
        return ShoppingCart.builder()
                .userId(userId)
                .itemsQuantity(DEFAULT_ITEMS_QUANTITY)
                .productsQuantity(DEFAULT_PRODUCTS_QUANTITY)
                .items(new HashSet<>())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
