package com.zufar.onlinestore.cart.repository;

import com.zufar.onlinestore.cart.entity.ShoppingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShoppingSessionRepository extends JpaRepository<ShoppingSession, UUID> {
}