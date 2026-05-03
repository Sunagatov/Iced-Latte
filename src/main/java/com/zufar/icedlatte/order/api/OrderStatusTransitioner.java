package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.event.OrderStatusChangedEvent;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.validator.OrderStatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusTransitioner {

    private final OrderRepository orderRepository;
    private final OrderStateMachine stateMachine;
    private final OrderStatusTransitionValidator validator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order transition(UUID orderId, OrderEvent event, UUID actorId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        OrderStatus oldStatus = order.getStatus();
        validator.validate(order, event, actorId);
        OrderStatus newStatus = stateMachine.transition(oldStatus, event);

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("order.status.transitioned: orderId={}, from={}, to={}, event={}, actor={}",
                orderId, oldStatus, newStatus, event, actorId);

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                orderId, oldStatus, newStatus, actorId, reason, OffsetDateTime.now()
        ));

        return saved;
    }

    @Transactional
    public Order transition(UUID orderId, OrderEvent event, UUID actorId) {
        return transition(orderId, event, actorId, null);
    }
}
