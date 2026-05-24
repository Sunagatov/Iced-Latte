package com.zufar.icedlatte.order.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zufar.icedlatte.order.entity.OrderStatusHistory;

@Repository
@SuppressWarnings("unused") // Spring Data generates implementations for repository methods.
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    List<OrderStatusHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);
}
