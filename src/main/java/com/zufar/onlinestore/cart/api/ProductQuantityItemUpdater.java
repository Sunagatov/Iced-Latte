package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.openapi.dto.ShoppingSessionDto;
import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.cart.exception.InvalidItemProductQuantityException;
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
public class ProductQuantityItemUpdater {

    private final ShoppingSessionItemRepository shoppingSessionItemRepository;
    private final ShoppingSessionProvider shoppingSessionProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingSessionDto update(final UUID shoppingSessionItemId,
                                     final int productQuantityChange) throws ShoppingSessionNotFoundException, ShoppingSessionItemNotFoundException {
        ShoppingSessionItem item = getShoppingSessionItem(shoppingSessionItemId);
        ShoppingSessionItem updatedItem = updateItemProductQuantity(shoppingSessionItemId, productQuantityChange, item);
        ShoppingSessionDto shoppingSession = getShoppingSession();

        if (shoppingSession.getId() != updatedItem.getShoppingSession().getId()) {
            log.warn("Failed to update the productQuantity with the change = {} in the shoppingSessionItem with id: {} of the shoppingSession with the id = {}.",
                    productQuantityChange, shoppingSessionItemId, shoppingSession.getId());
            throw new InvalidShoppingSessionIdException(shoppingSession.getId());
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
                                                          int productQuantityChange,
                                                          ShoppingSessionItem item) {
        int newQuantity = item.getProductQuantity() + productQuantityChange;
        if (newQuantity < 0) {
            log.warn("Attempted to set negative products quantity for item with id: {}.", shoppingSessionItemId);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
        if (productQuantityChange == 0) {
            log.warn("productQuantityChange for item with id: {} must be not equal to zero.", shoppingSessionItemId);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
        item.setProductQuantity(newQuantity);

        return shoppingSessionItemRepository.save(item);
    }

    private ShoppingSessionDto getShoppingSession() throws ShoppingSessionNotFoundException {
        UserDto userDto = securityPrincipalProvider.get();
        UUID userId = userDto.getId();
        return shoppingSessionProvider.getByUserId(userId);
    }
}
