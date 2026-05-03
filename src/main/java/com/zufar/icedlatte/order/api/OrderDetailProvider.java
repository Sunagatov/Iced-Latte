package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderDetailProvider {

    private static final Set<OrderStatus> CANCELLABLE = Set.of(OrderStatus.CREATED, OrderStatus.PAID);

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;

    @Transactional(readOnly = true)
    public OrderDto getOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException(orderId);
        }

        OrderDto dto = orderDtoConverter.toResponseDto(order);
        dto.setCanCancel(canCancel(order));
        dto.setCanRefund(order.getStatus() == OrderStatus.PAID);
        dto.setCancellationDeadline(order.getCancellationDeadline());
        return dto;
    }

    private boolean canCancel(Order order) {
        return CANCELLABLE.contains(order.getStatus())
                && order.getCancellationDeadline() != null
                && OffsetDateTime.now().isBefore(order.getCancellationDeadline());
    }
}
