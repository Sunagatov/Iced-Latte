package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.repository.ShoppingCartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartCreator {

    public static final int DEFAULT_PRODUCTS_QUANTITY = 0;
    public static final int DEFAULT_ITEMS_QUANTITY = 0;

    private final ShoppingCartRepository shoppingCartRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public ShoppingCart createNewShoppingCart(UUID userId) {
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .itemsQuantity(DEFAULT_ITEMS_QUANTITY)
                .productsQuantity(DEFAULT_PRODUCTS_QUANTITY)
                .items(new HashSet<>())
                .createdAt(OffsetDateTime.now())
                .build();

        this.shoppingCartRepository.save(shoppingCart);

        log.info("The new shopping cart was created and saved in database.");
        return shoppingCart;
    }
}
