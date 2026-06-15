package com.zufar.icedlatte.order.service.lifecycle;

import static com.zufar.icedlatte.openapi.dto.OrderEvent.CANCEL;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.DELIVER;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.PAYMENT_CONFIRMED;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.PAYMENT_EXPIRED_EVENT;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.PAYMENT_FAILED_EVENT;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.PENDING_PAYMENT_CONFIRMED;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.REFUND_CONFIRMED;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.REQUEST_REFUND;
import static com.zufar.icedlatte.openapi.dto.OrderEvent.SHIP;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.CANCELLED;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.CREATED;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.DELIVERED;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.PAID;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.PAYMENT_EXPIRED;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.PAYMENT_FAILED;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.PENDING_PAYMENT;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.REFUNDED;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.REFUND_REQUESTED;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.SHIPPED;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.event.OrderStatusChangedEvent;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderCancellationWindowExpiredException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatusTransitioner {

    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS = Map.of(
            PENDING_PAYMENT,
                    Map.of(
                            PENDING_PAYMENT_CONFIRMED, PAID,
                            PAYMENT_FAILED_EVENT, PAYMENT_FAILED,
                            PAYMENT_EXPIRED_EVENT, PAYMENT_EXPIRED,
                            CANCEL, CANCELLED),
            CREATED, Map.of(PAYMENT_CONFIRMED, PAID, CANCEL, CANCELLED),
            PAID, Map.of(SHIP, SHIPPED, CANCEL, CANCELLED, REQUEST_REFUND, REFUND_REQUESTED),
            SHIPPED, Map.of(DELIVER, DELIVERED),
            REFUND_REQUESTED, Map.of(REFUND_CONFIRMED, REFUNDED));

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderDtoConverter orderDtoConverter;

    public Order transition(UUID orderId, OrderEvent event, UUID actorId) {
        return transition(orderId, event, actorId, null);
    }

    /** Transitions order state. Returns the updated Order entity for internal use. */
    @Transactional
    public Order transition(UUID orderId, OrderEvent event, UUID actorId, String reason) {
        return transition(orderId, event, actorId, reason, true);
    }

    @Transactional
    public Order expireUnpaid(UUID orderId, String reason) {
        return transition(orderId, CANCEL, null, reason, false);
    }

    private Order transition(
            UUID orderId, OrderEvent event, UUID actorId, String reason, boolean enforceCancellationDeadline) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        validateGuards(order, event, actorId, enforceCancellationDeadline);

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = resolveTransition(oldStatus, event);

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        String logMessage = "order.status.transitioned: orderId={}, from={}, to={}, event={}, actor={}";
        log.info(logMessage, orderId, oldStatus, newStatus, event, actorId);

        OrderStatusChangedEvent orderStatusChangedEvent =
                new OrderStatusChangedEvent(orderId, oldStatus, newStatus, actorId, reason, OffsetDateTime.now());
        eventPublisher.publishEvent(orderStatusChangedEvent);

        return saved;
    }

    @Transactional
    public OrderDto cancel(UUID orderId, UUID userId) {
        Order order = requireOwnedOrder(orderId, userId);
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;
        Order cancelled = transition(orderId, CANCEL, userId, "User cancelled");
        if (wasPaid) {
            String logMessage = "order.cancel.refund_needed: orderId={}, stripePaymentIntentId={}";
            log.warn(logMessage, orderId, order.getStripePaymentIntentId());
        }
        return orderDtoConverter.toResponseDto(cancelled);
    }

    @Transactional
    public OrderDto requestRefund(UUID orderId, UUID userId, String reason) {
        requireOwnedOrder(orderId, userId);
        Order refundRequested = transition(orderId, REQUEST_REFUND, userId, reason);
        refundRequested.setRefundReason(reason);
        orderRepository.save(refundRequested);
        String logMessage = "order.refund.requested: orderId={}, stripePaymentIntentId={}";
        log.info(logMessage, orderId, refundRequested.getStripePaymentIntentId());
        return orderDtoConverter.toResponseDto(refundRequested);
    }

    private static OrderStatus resolveTransition(OrderStatus current, OrderEvent event) {
        return Optional.ofNullable(TRANSITIONS.get(current))
                .map(events -> events.get(event))
                .orElseThrow(() -> new InvalidOrderStateTransitionException(current, event));
    }

    private static void validateGuards(
            Order order, OrderEvent event, UUID actorId, boolean enforceCancellationDeadline) {
        if (enforceCancellationDeadline
                && event == CANCEL
                && order.getCancellationDeadline() != null
                && OffsetDateTime.now().isAfter(order.getCancellationDeadline())) {
            throw new OrderCancellationWindowExpiredException(order.getId());
        }
        if (event == REQUEST_REFUND && !order.getUserId().equals(actorId)) {
            throw new OrderAccessDeniedException();
        }
    }

    private Order requireOwnedOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }
        return order;
    }
}
