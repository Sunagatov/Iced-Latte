package com.zufar.icedlatte.cart.repository;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ShoppingCartItemRepository extends JpaRepository<ShoppingCartItem, UUID> {

    @Modifying
    @Query(value = "UPDATE shopping_cart_item SET products_quantity = products_quantity + :product_quantity_change WHERE id = :shopping_cart_item_id RETURNING *",
            nativeQuery = true)
    Integer updateProductsQuantityInShoppingCartItem(@Param("shopping_cart_item_id") UUID shoppingCartItemId,
                                                     @Param("product_quantity_change") Integer productQuantityChange);
}