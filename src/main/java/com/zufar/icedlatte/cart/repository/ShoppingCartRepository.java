package com.zufar.icedlatte.cart.repository;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.UUID;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, UUID> {

    @EntityGraph(type = EntityGraph.EntityGraphType.FETCH, attributePaths = {"items"})
    Optional<ShoppingCart> findShoppingCartByUserId(UUID userId);

    @Modifying(flushAutomatically = true)
    void deleteByUserId(UUID userId);
}