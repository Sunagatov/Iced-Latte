package com.zufar.icedlatte.cart.repository;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShoppingCartItemRepository extends JpaRepository<ShoppingCartItem, UUID> {

    @Modifying
    @Query("DELETE FROM ShoppingCartItem i WHERE i.id IN :itemIds AND i.shoppingCart.userId = :userId")
    void deleteByIdInAndUserId(@Param("itemIds") List<UUID> itemIds, @Param("userId") UUID userId);
}