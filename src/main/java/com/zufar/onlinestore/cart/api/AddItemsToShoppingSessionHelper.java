package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.NewShoppingSessionItemDto;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddItemsToShoppingSessionHelper {

    public static final int DEFAULT_PRODUCTS_QUANTITY = 0;
    public static final int DEFAULT_ITEMS_QUANTITY = 0;

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final SecurityPrincipalProvider securityPrincipalProvider;
    private final ProductInfoRepository productInfoRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto add(final Set<NewShoppingSessionItemDto> items) {
        UUID userId = securityPrincipalProvider.getUserId();

        ShoppingSession shoppingSession = Optional.ofNullable(shoppingSessionRepository.findShoppingSessionByUserId(userId))
                .orElseGet(() -> createNewShoppingSession(userId));

        Set<ShoppingSessionItem> shoppingSessionItems = createShoppingSessionItems(items, shoppingSession);

        ShoppingSession updatedShoppingSession = updateExistingShoppingSession(shoppingSession, shoppingSessionItems);

        ShoppingSession persistedShoppingSession = shoppingSessionRepository.save(updatedShoppingSession);
        return shoppingSessionDtoConverter.toDto(persistedShoppingSession);
    }

    private Set<ShoppingSessionItem> createShoppingSessionItems(Set<NewShoppingSessionItemDto> items, ShoppingSession shoppingSession) {
        Map<UUID, Integer> productsWithQuantity = items
                .stream()
                .collect(Collectors.toMap(NewShoppingSessionItemDto::productId, NewShoppingSessionItemDto::productsQuantity));
        Set<UUID> productIds = productsWithQuantity.keySet();

        return productInfoRepository.findAllById(productIds).stream()
                .map(productInfo -> ShoppingSessionItem.builder()
                        .shoppingSession(shoppingSession)
                        .productsQuantity(productsWithQuantity.get(productInfo.getProductId()))
                        .productInfo(productInfo)
                        .build())
                .collect(Collectors.toSet());
    }

    private static ShoppingSession updateExistingShoppingSession(ShoppingSession existingShoppingSession, Set<ShoppingSessionItem> shoppingSessionItems) {
        int productsQuantity = shoppingSessionItems.stream()
                .map(ShoppingSessionItem::getProductsQuantity)
                .reduce(Integer::sum)
                .orElse(DEFAULT_PRODUCTS_QUANTITY);

        existingShoppingSession.setItemsQuantity(existingShoppingSession.getItemsQuantity() + shoppingSessionItems.size());
        existingShoppingSession.setProductsQuantity(existingShoppingSession.getProductsQuantity() + productsQuantity);
        existingShoppingSession.getItems().addAll(shoppingSessionItems);
        return existingShoppingSession;
    }

    private static ShoppingSession createNewShoppingSession(UUID userId) {
        return ShoppingSession.builder()
                .userId(userId)
                .itemsQuantity(DEFAULT_ITEMS_QUANTITY)
                .productsQuantity(DEFAULT_PRODUCTS_QUANTITY)
                .items(Collections.emptySet())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
