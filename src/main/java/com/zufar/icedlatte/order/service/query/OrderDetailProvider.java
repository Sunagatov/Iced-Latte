package com.zufar.icedlatte.order.service.query;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderPageDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.openapi.dto.OrderStatusHistoryDto;
import com.zufar.icedlatte.openapi.dto.OrderSummaryDto;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.order.repository.OrderStatusHistoryRepository;
import com.zufar.icedlatte.order.service.OrderCreator;
import com.zufar.icedlatte.order.service.lifecycle.OrderStatusTransitioner;
import com.zufar.icedlatte.order.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderDetailProvider implements OrderPaymentApi {

    private static final Set<OrderStatus> CANCELLABLE = Set.of(OrderStatus.CREATED, OrderStatus.PAID);

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderStatusTransitioner orderStatusTransitioner;

    @Override
    @Transactional(readOnly = true)
    public OrderSnapshot getSnapshot(UUID orderId) {
        return toSnapshot(findById(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSnapshot getSnapshotWithItems(UUID orderId) {
        return toSnapshot(findByIdWithItems(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderSnapshot> findByStripePaymentIntentId(String paymentIntentId) {
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

    @Transactional(readOnly = true)
    public OrderPageDto getOrders(UUID userId,
                                  List<OrderStatus> statuses,
                                  Integer year,
                                  LocalDate dateFrom,
                                  LocalDate dateTo,
                                  Pageable pageable) {
        Specification<Order> spec = Specification.where(OrderSpecifications.belongsToUser(userId));

        Specification<Order> statusSpec = OrderSpecifications.hasStatusIn(statuses);
        if (statusSpec != null) spec = spec.and(statusSpec);

        Specification<Order> yearSpec = OrderSpecifications.createdInYear(year);
        if (yearSpec != null) spec = spec.and(yearSpec);

        Specification<Order> fromSpec = OrderSpecifications.createdAfter(dateFrom);
        if (fromSpec != null) spec = spec.and(fromSpec);

        Specification<Order> toSpec = OrderSpecifications.createdBefore(dateTo);
        if (toSpec != null) spec = spec.and(toSpec);

        Page<Order> page = orderRepository.findAll(spec, pageable);

        List<OrderSummaryDto> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new OrderPageDto()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages());
    }

    Order findById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    Order findByIdWithItems(UUID orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    @Transactional
    public void confirmPayment(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.PENDING_PAYMENT_CONFIRMED, null, reason);
    }

    @Override
    @Transactional
    public void expirePayment(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.PAYMENT_EXPIRED_EVENT, null, reason);
    }

    @Override
    @Transactional
    public void failPayment(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.PAYMENT_FAILED_EVENT, null, reason);
    }

    @Override
    @Transactional
    public void assignPaymentIntent(UUID orderId, String stripePaymentIntentId) {
        Order order = findById(orderId);
        order.setStripePaymentIntentId(stripePaymentIntentId);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void confirmRefund(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.REFUND_CONFIRMED, null, reason);
    }

    private OrderSnapshot toSnapshot(Order order) {
        List<OrderSnapshot.OrderItemSnapshot> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                .map(i -> new OrderSnapshot.OrderItemSnapshot(i.getProductName(), i.getProductPrice(), i.getProductsQuantity()))
                .toList();
        return new OrderSnapshot(order.getId(), order.getUserId(), OrderCreator.toStatusSnapshot(order.getStatus()),
                order.getItemsTotalPrice(), order.getStripePaymentIntentId(), items);
    }

    private boolean canCancel(Order order) {
        return CANCELLABLE.contains(order.getStatus())
                && order.getCancellationDeadline() != null
                && OffsetDateTime.now().isBefore(order.getCancellationDeadline());
    }

    private OrderSummaryDto toSummary(Order order) {
        String firstItemName = order.getItems() != null && !order.getItems().isEmpty()
                ? order.getItems().getFirst().getProductName()
                : null;

        return new OrderSummaryDto()
                .id(order.getId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .itemsQuantity(order.getItemsQuantity())
                .itemsTotalPrice(order.getItemsTotalPrice() != null ? order.getItemsTotalPrice().doubleValue() : null)
                .firstItemName(firstItemName)
                .itemCount(order.getItems() != null ? order.getItems().size() : 0);
    }
}
