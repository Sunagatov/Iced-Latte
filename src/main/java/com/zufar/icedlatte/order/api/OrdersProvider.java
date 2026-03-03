package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrdersProvider {

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public List<OrderDto> getOrders(final UUID userId, final List<OrderStatus> statuses) {
        List<com.zufar.icedlatte.order.entity.Order> orders = (statuses == null || statuses.isEmpty())
                ? orderRepository.findAllByUserId(userId)
                : orderRepository.findAllByUserIdAndStatusIn(userId, statuses);
        return orders.stream().map(orderDtoConverter::toResponseDto).toList();
    }
}
