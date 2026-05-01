package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQuantityItemUpdater {

    private final ShoppingCartItemRepository shoppingCartItemRepository;
    private final ShoppingCartProvider shoppingCartProvider;

    @Retryable(retryFor = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto update(final UUID shoppingCartItemId,
                                  final UUID userId,
                                  final int productQuantityChange) throws ShoppingCartNotFoundException, ShoppingCartItemNotFoundException {
        // Scoped lookup: returns 404 for both nonexistent and foreign items — no ownership info leaked
        ShoppingCartItem item = shoppingCartItemRepository.findByIdAndShoppingCartUserId(shoppingCartItemId, userId)
                .orElseThrow(() -> new ShoppingCartItemNotFoundException(shoppingCartItemId));

        validateQuantityChange(shoppingCartItemId, productQuantityChange, item);

        item.setProductQuantity(item.getProductQuantity() + productQuantityChange);
        shoppingCartItemRepository.save(item);
        return shoppingCartProvider.getByUserId(userId);
    }

    private void validateQuantityChange(final UUID shoppingCartItemId,
                                        int productQuantityChange,
                                        ShoppingCartItem item) {
        int newQuantity = item.getProductQuantity() + productQuantityChange;
        if (newQuantity < 1) {
            log.debug("cart.item.quantity.invalid: itemId={}, quantity={}", shoppingCartItemId, newQuantity);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
        if (productQuantityChange == 0) {
            log.debug("cart.item.quantity.zero_change: itemId={}", shoppingCartItemId);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
    }
}
