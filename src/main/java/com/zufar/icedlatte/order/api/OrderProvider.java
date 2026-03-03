package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderProvider {

    private final OrderRepository orderRepository;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.SERIALIZABLE, readOnly = true)
    public Optional<Order> getOrderEntityByUserAndSession(final UUID userId, final String sessionId) {
        return orderRepository.findByUserIdAndSessionId(userId, sessionId);
    }
}
