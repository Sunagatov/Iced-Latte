package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartCreator {

    private final ShoppingCartRepository shoppingCartRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCart getOrCreate(UUID userId) {
        return shoppingCartRepository.findShoppingCartByUserId(userId)
                .orElseGet(() -> createNewShoppingCart(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCart createNewShoppingCart(UUID userId) {
        try {
            ShoppingCart shoppingCart = ShoppingCart.builder()
                    .userId(userId)
                    .items(new HashSet<>())
                    .build();
            shoppingCartRepository.save(shoppingCart);
            log.info("cart.created: userId={}", userId);
            return shoppingCart;
        } catch (DataIntegrityViolationException ex) {
            // Another concurrent request created the cart between our read and insert.
            // The unique constraint on user_id prevented a duplicate — re-read the winner.
            log.warn("cart.create.concurrent_conflict: userId={}", userId);
            return shoppingCartRepository.findShoppingCartByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cart not found after uniqueness conflict for userId=" + userId, ex));
        }
    }
}
