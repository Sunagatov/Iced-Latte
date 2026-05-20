package com.zufar.icedlatte.order.internal.query;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.OrderStatusHistoryDto;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderDetailProvider {

    private static final Set<OrderStatus> CANCELLABLE = Set.of(OrderStatus.CREATED, OrderStatus.PAID);

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Transactional(readOnly = true)
    public OrderSnapshot getSnapshot(UUID orderId) {
        return toSnapshot(findById(orderId));
    }

    @Transactional(readOnly = true)
    public OrderSnapshot getSnapshotWithItems(UUID orderId) {
        return toSnapshot(findByIdWithItems(orderId));
    }

    @Transactional(readOnly = true)
    public Optional<OrderSnapshot> findSnapshotByStripePaymentIntentId(String paymentIntentId) {
        return orderRepository.findByStripePaymentIntentId(paymentIntentId).map(this::toSnapshot);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }

        OrderDto dto = orderDtoConverter.toResponseDto(order);
        dto.setCanCancel(canCancel(order));
        dto.setCanRefund(order.getStatus() == OrderStatus.PAID);
        dto.setCancellationDeadline(order.getCancellationDeadline());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDto> getOrderHistory(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId).stream()
                .map(h -> new OrderStatusHistoryDto()
                        .id(h.getId())
                        .orderId(h.getOrderId())
                        .oldStatus(h.getOldStatus())
                        .newStatus(h.getNewStatus())
                        .changedBy(h.getChangedBy())
                        .reason(h.getReason())
                        .changedAt(h.getChangedAt()))
                .toList();
    }

    Order findById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    Order findByIdWithItems(UUID orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderSnapshot toSnapshot(Order order) {
        List<OrderSnapshot.OrderItemSnapshot> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                    .map(i -> new OrderSnapshot.OrderItemSnapshot(i.getProductName(), i.getProductPrice(), i.getProductsQuantity()))
                    .toList();
        return new OrderSnapshot(order.getId(), order.getUserId(), order.getStatus(),
                order.getItemsTotalPrice(), order.getStripePaymentIntentId(), items);
    }

    private boolean canCancel(Order order) {
        return CANCELLABLE.contains(order.getStatus())
                && order.getCancellationDeadline() != null
                && OffsetDateTime.now().isBefore(order.getCancellationDeadline());
    }
}
