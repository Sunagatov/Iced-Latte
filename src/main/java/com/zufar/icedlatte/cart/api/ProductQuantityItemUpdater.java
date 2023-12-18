package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.InvalidShoppingCartIdException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import com.zufar.icedlatte.openapi.dto.UserDto;
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

    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ShoppingCartProvider shoppingCartProvider;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto update(final UUID shoppingCartItemId,
                                  final int productQuantityChange) throws ShoppingCartNotFoundException, ShoppingCartItemNotFoundException {
        ShoppingCartItem item = getShoppingCartItem(shoppingCartItemId);
        ShoppingCartItem updatedItem = updateItemProductQuantity(shoppingCartItemId, productQuantityChange, item);
        ShoppingCartDto shoppingCart = getShoppingCart();

        if (shoppingCart.getId() != updatedItem.getShoppingCart().getId()) {
            log.warn("Failed to update the productQuantity with the change = {} in the shoppingCartItem with id: {} of the shoppingCart with the id = {}.",
                    productQuantityChange, shoppingCartItemId, shoppingCart.getId());
            throw new InvalidShoppingCartIdException(shoppingCart.getId());
        }
        return shoppingCart;
    }

    private ShoppingCartItem getShoppingCartItem(final UUID shoppingCartItemId) throws ShoppingCartItemNotFoundException {
        return shoppingCartItemRepository.findById(shoppingCartItemId)
                .orElseThrow(() -> {
                    log.warn("Shopping cart item  with id = {} is not found.", shoppingCartItemId);
                    return new ShoppingCartItemNotFoundException(shoppingCartItemId);
                });
    }

    private ShoppingCartItem updateItemProductQuantity(final UUID shoppingCartItemId,
                                                       int productQuantityChange,
                                                       ShoppingCartItem item) {
        int newQuantity = item.getProductQuantity() + productQuantityChange;
        if (newQuantity < 0) {
            log.warn("Attempted to set negative products quantity for item with id: {}.", shoppingCartItemId);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
        if (productQuantityChange == 0) {
            log.warn("productQuantityChange for item with id: {} must be not equal to zero.", shoppingCartItemId);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
        item.setProductQuantity(newQuantity);

        return shoppingCartItemRepository.save(item);
    }

    private ShoppingCartDto getShoppingCart() throws ShoppingCartNotFoundException {
        UserDto userDto = securityPrincipalProvider.get();
        UUID userId = userDto.getId();
        return shoppingCartProvider.getByUserId(userId);
    }
}
