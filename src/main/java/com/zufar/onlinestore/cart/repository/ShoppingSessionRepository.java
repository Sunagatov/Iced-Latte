package com.zufar.onlinestore.cart.repository;

import com.zufar.onlinestore.cart.entity.ShoppingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingSessionRepository extends JpaRepository<ShoppingSession, UUID> {

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"items", "items.productInfo"})
    ShoppingSession findShoppingSessionByUserId(UUID userId);
}