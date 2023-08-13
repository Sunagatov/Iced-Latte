package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.api.ShoppingSessionProvider;
import com.zufar.onlinestore.cart.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.InvalidItemProductsQuantityException;
import com.zufar.onlinestore.cart.exception.InvalidShoppingSessionIdException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionItemNotFoundException;
import com.zufar.onlinestore.cart.exception.ShoppingSessionNotFoundException;
import com.zufar.onlinestore.cart.repository.ShoppingSessionItemRepository;
import com.zufar.onlinestore.security.api.SecurityPrincipalProvider;
import com.zufar.onlinestore.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductsQuantityItemUpdater {

    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto update(final UUID shoppingSessionItemId,
                                     final int productsQuantityChange) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException {
        ShoppingSessionItem item = getShoppingSessionItem(shoppingSessionItemId);
        ShoppingSessionItem updatedItem = updateItemProductQuantity(shoppingSessionItemId, productsQuantityChange, item);
        ShoppingSessionDto shoppingSession = getShoppingSession();

        if (shoppingSession.id() != updatedItem.getShoppingSession().getId()) {
            log.warn("Failed to update the productsQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                    productsQuantityChange, shoppingSessionItemId, shoppingSession.id());
            throw new InvalidShoppingSessionIdException(shoppingSession.id());
        }

        return shoppingSession;
    }

    private ShoppingSessionItem getShoppingSessionItem(final UUID shoppingSessionItemId) throws ShoppingSessionItemNotFoundException {
        return shoppingSessionItemRepository.findById(shoppingSessionItemId)
                .orElseThrow(() -> {
                    log.warn("Shopping session item  with id = {} is not found.", shoppingSessionItemId);
                    return new ShoppingSessionItemNotFoundException(shoppingSessionItemId);
                });
    }

    private ShoppingSessionItem updateItemProductQuantity(final UUID shoppingSessionItemId,
                                       int productsQuantityChange,
                                       ShoppingSessionItem item) {
        int newQuantity = item.getProductsQuantity() + productsQuantityChange;
        if (newQuantity < 0) {
            log.warn("Attempted to set negative products quantity for item with id: {}.", shoppingSessionItemId);
            throw new InvalidItemProductsQuantityException(newQuantity);
        }
        item.setProductsQuantity(newQuantity);

        return shoppingSessionItemRepository.save(item);
    }

    private ShoppingSessionDto getShoppingSession() throws ShoppingSessionNotFoundException {
        UserDto userDto = securityPrincipalProvider.get();
        UUID userId = userDto.userId();
        return shoppingSessionProvider.getByUserId(userId);
    }
}

