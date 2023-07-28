package com.zufar.onlinestore.cart.api.service;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
import com.zufar.onlinestore.product.entity.ProductInfo;
import com.zufar.onlinestore.product.exception.ProductNotFoundException;
import com.zufar.onlinestore.product.repository.ProductInfoRepository;
import com.zufar.onlinestore.user.api.UserApi;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingSessionItemSaver {

    private static final int DEFAULT_PRODUCTS_QUANTITY_WHEN_NEW_ITEM_IS_CREATED = 1;

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ProductInfoRepository productInfoRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;
    private final UserApi userApi;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto save(UUID userId, UUID productId) {
        userApi.getUserById(userId);

        ProductInfo productInfo = productInfoRepository.findById(productId).get();

        ShoppingSession shoppingSession = shoppingSessionRepository.findShoppingSessionByUserId(userId);
        if (shoppingSession == null) {
            createNewShoppingSession(userId, productInfo);
        }

      throw new UnsupportedOperationException();
    }

    private void createNewShoppingSession(UUID userId, ProductInfo productInfo) {
        ShoppingSessionItem newShoppingSessionItem = ShoppingSessionItem.builder()
                .productInfo(productInfo)
                .productsQuantity(1)
                .build();

        ShoppingSession newShoppingSession = ShoppingSession.builder()
                .userId(userId)
                .itemsQuantity(1)
                .productsQuantity(1)
                .items(Collections.singleton(newShoppingSessionItem))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ShoppingSessionItem createShoppingSessionItemEntity(UUID shoppingSessionItemId, UUID shoppingSessionId, UUID productId) {
        ShoppingSession shoppingSessionEntity = getShoppingSessionEntityFromDatabase(shoppingSessionItemId, shoppingSessionId);
        ProductInfo productInfo = getProductInfoEntityFromDatabase(productId);

        return ShoppingSessionItem.builder()
                .shoppingSession(shoppingSessionEntity)
                .productInfo(productInfo)
                .productsQuantity(DEFAULT_PRODUCTS_QUANTITY_WHEN_NEW_ITEM_IS_CREATED)
                .build();
    }

    private ShoppingSession getShoppingSessionEntityFromDatabase(UUID shoppingSessionItemId, UUID shoppingSessionId) {
        Optional<ShoppingSession> shoppingSession = shoppingSessionRepository.findById(shoppingSessionId);
        if (shoppingSession.isEmpty()) {
            log.warn("Failed to add a new shoppingSessionItem with the id = {} to the shoppingSession with the id = {} because the shoppingSession with the id = {}  is absent",
                    shoppingSessionItemId, shoppingSessionId, shoppingSessionId);
            throw new ShoppingSessionNotFoundException(shoppingSessionId);
        }
        return shoppingSession.get();
    }

    private ProductInfo getProductInfoEntityFromDatabase(final UUID productId) {
        Optional<ProductInfo> productInfo = productInfoRepository.findById(productId);
        if (productInfo.isEmpty()) {
            log.warn("Failed to add a new shoppingSessionItem to the shoppingSession because the product with the id = {} is absent", productId);
//            throw new ProductNotFoundException(productId);
        }
        return productInfo.get();
    }

    private ShoppingSessionItem getShoppingSessionItemEntityFromDatabase(UUID shoppingSessionItemId, UUID shoppingSessionId) {
        Optional<ShoppingSessionItem> shoppingSessionItem = shoppingSessionItemRepository.findById(shoppingSessionItemId);
        if (shoppingSessionItem.isEmpty()) {
            log.warn("Failed to add a new shoppingSessionItem with the id = {} to the shoppingSession with the id = {} because the shoppingSessionItem with the id = {} is absent",
                    shoppingSessionItemId, shoppingSessionId, shoppingSessionItemId);
            throw new ShoppingSessionItemNotFoundException(shoppingSessionId, shoppingSessionItemId);
        }
        return shoppingSessionItem.get();
    }
}
