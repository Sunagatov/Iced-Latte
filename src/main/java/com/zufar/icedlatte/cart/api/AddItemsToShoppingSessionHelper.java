package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.icedlatte.openapi.dto.NewShoppingSessionItemDto;
import com.zufar.icedlatte.openapi.dto.ShoppingSessionDto;
import com.zufar.icedlatte.cart.entity.ShoppingSession;
import com.zufar.icedlatte.cart.entity.ShoppingSessionItem;
import com.zufar.icedlatte.cart.repository.ShoppingSessionRepository;
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
import java.util.Collections;
import java.util.List;
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
    public ShoppingSessionDto add(final Set<NewShoppingSessionItemDto> itemsToAdd) {
        UUID userId = securityPrincipalProvider.getUserId();

        ShoppingSession shoppingSession = Optional.ofNullable(shoppingSessionRepository.findShoppingSessionByUserId(userId))
                .orElseGet(() -> createNewShoppingSession(userId));

        List<ShoppingSessionItem> items = createItems(itemsToAdd, shoppingSession);

        ShoppingSession updatedShoppingSession = updateExistingShoppingSession(shoppingSession, items);

        ShoppingSession persistedShoppingSession = shoppingSessionRepository.save(updatedShoppingSession);
        return shoppingSessionDtoConverter.toDto(persistedShoppingSession);
    }

    private List<ShoppingSessionItem> createItems(Set<NewShoppingSessionItemDto> itemsToAdd, ShoppingSession shoppingSession) {
        Map<UUID, Integer> productsWithQuantity = itemsToAdd.stream()
                .collect(Collectors.toMap(NewShoppingSessionItemDto::getProductId, NewShoppingSessionItemDto::getProductQuantity));

        Set<UUID> existedProductIds = shoppingSession.getItems().stream()
                .map(ShoppingSessionItem::getProductInfo)
                .map(ProductInfo::getProductId)
                .collect(Collectors.toSet());

        Set<UUID> newProductIds = productsWithQuantity.keySet().stream()
                .filter(productId -> !existedProductIds.contains(productId))
                .collect(Collectors.toSet());

        return productInfoRepository.findAllById(newProductIds).stream()
                .map(productInfo -> ShoppingSessionItem.builder()
                        .shoppingSession(shoppingSession)
                        .productQuantity(productsWithQuantity.get(productInfo.getProductId()))
                        .productInfo(productInfo)
                        .build())
                .toList();
    }

    private static ShoppingSession updateExistingShoppingSession(ShoppingSession existingShoppingSession,
                                                                 List<ShoppingSessionItem> shoppingSessionItems) {
        int productsQuantity = shoppingSessionItems.stream()
                .map(ShoppingSessionItem::getProductQuantity)
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
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
