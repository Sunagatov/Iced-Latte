package com.zufar.onlinestore.cart.api.service;

import com.zufar.onlinestore.cart.converter.ShoppingSessionDtoConverter;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdInUpdateProductsQuantityRequestException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.cart.repository.ShoppingSessionRepository;
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
public class ProductsQuantityItemUpdater {

    private final ShoppingSessionRepository shoppingSessionRepository;
    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionDtoConverter shoppingSessionDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto update(final UUID shoppingSessionId,
                                     final UUID shoppingSessionItemId,
                                     final Integer productsQuantityChange) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException, InvalidShoppingSessionIdInUpdateProductsQuantityRequestException {
        ShoppingSessionItem updatedItem = shoppingSessionItemRepository.updateProductsQuantityInShoppingSessionItem(shoppingSessionItemId, productsQuantityChange);

        if (updatedItem == null) {
            log.warn("Failed to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                    productsQuantityChange, shoppingSessionItemId, shoppingSessionId);
            throw new ShoppingSessionItemNotFoundException(shoppingSessionId, shoppingSessionItemId);
        }

        Optional<ShoppingSession> shoppingSession = shoppingSessionRepository.findById(shoppingSessionId);
        if (shoppingSession.isEmpty()) {
            log.warn("Failed to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                    productsQuantityChange, shoppingSessionItemId, shoppingSessionId);
            throw new ShoppingSessionNotFoundException(shoppingSessionId);
        }

        if (shoppingSessionId != updatedItem.getShoppingSession().getId()) {
            log.warn("Failed to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                    productsQuantityChange, shoppingSessionItemId, shoppingSessionId);
            throw new InvalidShoppingSessionIdInUpdateProductsQuantityRequestException(shoppingSessionId);
        }

        return shoppingSessionDtoConverter.toDto(shoppingSession.get());
    }
}
