package com.zufar.onlinestore.cart.repository;

import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ShoppingSessionItemRepository extends JpaRepository<ShoppingSessionItem, UUID> {

    @Modifying
    @Query(value = "UPDATE shopping_session_item SET products_quantity = products_quantity + :products_quantity_change WHERE id = :shopping_session_item_id RETURNING *",
            nativeQuery = true)
    ShoppingSessionItem updateProductsQuantityInShoppingSessionItem(@Param("shopping_session_item_id") UUID shoppingSessionItemId,
                                                                    @Param("products_quantity_change") Integer productsQuantityChange);
    @Query(value = "DELETE FROM shopping_session_item WHERE id = :shopping_session_item_id RETURNING *", nativeQuery = true)
    ShoppingSessionItem deleteShoppingSessionItemById(@Param("shopping_session_item_id") UUID shoppingSessionItemId);
}