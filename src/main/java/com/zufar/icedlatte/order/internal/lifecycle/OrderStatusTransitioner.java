package com.zufar.icedlatte.order.internal.lifecycle;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.event.OrderStatusChangedEvent;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderCancellationWindowExpiredException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.zufar.icedlatte.openapi.dto.OrderEvent.*;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusTransitioner {

    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT, Map.of(
                    PENDING_PAYMENT_CONFIRMED, PAID,
                    PAYMENT_FAILED_EVENT, PAYMENT_FAILED,
                    PAYMENT_EXPIRED_EVENT, PAYMENT_EXPIRED,
                    CANCEL, CANCELLED
            ),
            CREATED, Map.of(PAYMENT_CONFIRMED, PAID, CANCEL, CANCELLED),
            PAID, Map.of(SHIP, SHIPPED, CANCEL, CANCELLED, REQUEST_REFUND, REFUND_REQUESTED),
            OrderStatus.SHIPPED, Map.of(DELIVER, DELIVERED),
            REFUND_REQUESTED, Map.of(REFUND_CONFIRMED, REFUNDED)
    );

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order transition(UUID orderId, OrderEvent event, UUID actorId) {
        return transition(orderId, event, actorId, null);
    }

    /**
     * Transitions order state. Returns the updated Order entity for internal use.
     */
    @Transactional
    public Order transition(UUID orderId, OrderEvent event, UUID actorId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        validateGuards(order, event, actorId);

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = resolveTransition(oldStatus, event);

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        log.info("order.status.transitioned: orderId={}, from={}, to={}, event={}, actor={}",
                orderId, oldStatus, newStatus, event, actorId);

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                orderId, oldStatus, newStatus, actorId, reason, OffsetDateTime.now()
        ));

        return saved;
    }

    private static OrderStatus resolveTransition(OrderStatus current, OrderEvent event) {
        return Optional.ofNullable(TRANSITIONS.get(current))
                .map(events -> events.get(event))
                .orElseThrow(() -> new InvalidOrderStateTransitionException(current, event));
    }

    private static void validateGuards(Order order, OrderEvent event, UUID actorId) {
        if (event == OrderEvent.CANCEL
                && order.getCancellationDeadline() != null
                && OffsetDateTime.now().isAfter(order.getCancellationDeadline())) {
            throw new OrderCancellationWindowExpiredException(order.getId());
        }
        if (event == OrderEvent.REQUEST_REFUND && !order.getUserId().equals(actorId)) {
            throw new OrderAccessDeniedException();
        }
    }
}
