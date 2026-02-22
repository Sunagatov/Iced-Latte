package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import com.zufar.icedlatte.openapi.dto.ShoppingCartDto;
import com.zufar.icedlatte.cart.exception.InvalidItemProductQuantityException;
import com.zufar.icedlatte.cart.exception.InvalidShoppingCartIdException;
import com.zufar.icedlatte.cart.exception.ShoppingCartItemNotFoundException;
import com.zufar.icedlatte.cart.exception.ShoppingCartNotFoundException;
import com.zufar.icedlatte.cart.repository.ShoppingCartItemRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
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
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Retryable(retryFor = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 100))
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCartDto update(final UUID shoppingCartItemId,
                                  final int productQuantityChange) throws ShoppingCartNotFoundException, ShoppingCartItemNotFoundException {
        ShoppingCartItem item = getShoppingCartItem(shoppingCartItemId);
        ShoppingCartItem updatedItem = updateItemProductQuantity(shoppingCartItemId, productQuantityChange, item);
        ShoppingCartDto shoppingCart = getShoppingCart();

        if (!shoppingCart.getId().equals(updatedItem.getShoppingCart().getId())) {
        log.warn("cart.item.quantity.invalid: itemId={}, change={}, cartId={}",
                    productQuantityChange, shoppingCartItemId, shoppingCart.getId());
            throw new InvalidShoppingCartIdException(shoppingCart.getId());
        }
        return shoppingCart;
    }

    private ShoppingCartItem getShoppingCartItem(final UUID shoppingCartItemId) throws ShoppingCartItemNotFoundException {
        return shoppingCartItemRepository.findById(shoppingCartItemId)
                .orElseThrow(() -> new ShoppingCartItemNotFoundException(shoppingCartItemId));
    }

    private ShoppingCartItem updateItemProductQuantity(final UUID shoppingCartItemId,
                                                       int productQuantityChange,
                                                       ShoppingCartItem item) {
        int newQuantity = item.getProductQuantity() + productQuantityChange;
        if (newQuantity < 0) {
            log.warn("cart.item.quantity.negative: itemId={}, quantity={}", shoppingCartItemId, newQuantity);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
        if (productQuantityChange == 0) {
            log.warn("cart.item.quantity.zero_change: itemId={}", shoppingCartItemId);
            throw new InvalidItemProductQuantityException(newQuantity);
        }
        item.setProductQuantity(newQuantity);

        return shoppingCartItemRepository.save(item);
    }

    private ShoppingCartDto getShoppingCart() throws ShoppingCartNotFoundException {
        return shoppingCartProvider.getByUserId(securityPrincipalProvider.getUserId());
    }
}
