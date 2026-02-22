package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public static final int DEFAULT_ITEMS_QUANTITY = 0;

    private final ShoppingCartRepository shoppingCartRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCart getOrCreate(UUID userId) {
        return shoppingCartRepository.findShoppingCartByUserId(userId)
                .orElseGet(() -> createNewShoppingCart(userId));
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCart createNewShoppingCart(UUID userId) {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .itemsQuantity(DEFAULT_ITEMS_QUANTITY)
                .productsQuantity(ShoppingCart.DEFAULT_PRODUCTS_QUANTITY)
                .items(new HashSet<>())
                .build();

        shoppingCartRepository.save(shoppingCart);
        log.info("cart.created: userId={}", userId);
        return shoppingCart;
    }
}
