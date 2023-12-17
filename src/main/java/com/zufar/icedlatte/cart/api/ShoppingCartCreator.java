package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartCreator {

    public static final int DEFAULT_PRODUCTS_QUANTITY = 0;
    public static final int DEFAULT_ITEMS_QUANTITY = 0;

    public ShoppingCart createNewShoppingCart(UUID userId) {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .itemsQuantity(DEFAULT_ITEMS_QUANTITY)
                .productsQuantity(DEFAULT_PRODUCTS_QUANTITY)
                .items(new HashSet<>())
                .createdAt(OffsetDateTime.now())
                .build();

        log.info("The new shopping cart was created.");
        return shoppingCart;
    }
}
