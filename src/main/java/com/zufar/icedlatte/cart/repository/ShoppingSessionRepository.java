package com.zufar.icedlatte.cart.repository;

import com.zufar.icedlatte.cart.entity.ShoppingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;

public interface ShoppingSessionRepository extends JpaRepository<ShoppingSession, UUID> {

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"items", "items.productInfo"})
    ShoppingSession findShoppingSessionByUserId(UUID userId);
}