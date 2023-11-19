package com.zufar.icedlatte.cart.repository;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, UUID> {

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"items", "items.productInfo"})
    ShoppingCart findShoppingCartByUserId(UUID userId);
}