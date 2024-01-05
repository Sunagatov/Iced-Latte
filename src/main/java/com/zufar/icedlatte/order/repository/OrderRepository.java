package com.zufar.icedlatte.order.repository;

import com.zufar.icedlatte.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository  extends JpaRepository<Order, UUID> {
}
