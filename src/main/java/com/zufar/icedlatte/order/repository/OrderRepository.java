package com.zufar.icedlatte.order.repository;

import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByUserIdAndSessionId(UUID userId, String sessionId);

    List<Order> findAllByUserId(UUID userId);

    List<Order> findAllByUserIdAndStatusIn(UUID userId, List<OrderStatus> statuses);
}
