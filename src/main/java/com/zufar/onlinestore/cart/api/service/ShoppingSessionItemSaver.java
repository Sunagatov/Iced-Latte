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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto save(UUID shoppingSessionItemId, UUID shoppingSessionId, Integer productId) {
        ShoppingSessionItem shoppingSessionItemEntity;
        if (shoppingSessionItemId == null) {
            shoppingSessionItemEntity = createShoppingSessionItemEntity(shoppingSessionItemId, shoppingSessionId, productId);
        } else {
            shoppingSessionItemEntity = getShoppingSessionItemEntityFromDatabase(shoppingSessionItemId, shoppingSessionId);
        }
        shoppingSessionItemRepository.save(shoppingSessionItemEntity);
        ShoppingSession updatedShoppingSessionEntity = getShoppingSessionEntityFromDatabase(shoppingSessionItemId, shoppingSessionId);
        return shoppingSessionDtoConverter.toDto(updatedShoppingSessionEntity);
    }

    private ShoppingSessionItem createShoppingSessionItemEntity(UUID shoppingSessionItemId, UUID shoppingSessionId, Integer productId) {
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

    private ProductInfo getProductInfoEntityFromDatabase(final Integer productId) {
        Optional<ProductInfo> productInfo = productInfoRepository.findById(productId);
        if (productInfo.isEmpty()) {
            log.warn("Failed to add a new shoppingSessionItem to the shoppingSession because the product with the id = {} is absent", productId);
            throw new ProductNotFoundException(productId);
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
