package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderResponseDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProvider {

    private static final List<OrderStatus> DEFAULT_STATUS_LIST =
            List.of(OrderStatus.CREATED, OrderStatus.DELIVERY, OrderStatus.FINISHED);

    private final OrderRepository orderRepository;
    private final OrderDtoConverter orderDtoConverter;
    private final SecurityPrincipalProvider securityPrincipalProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public List<OrderResponseDto> getOrdersByStatus(final List<OrderStatus> statusList) {
        var userId = securityPrincipalProvider.getUserId();
        return orderRepository
                .findAllByUserIdAndStatus(userId, statusList == null ? DEFAULT_STATUS_LIST : statusList)
                .stream()
                .map(orderDtoConverter::toResponseDto)
                .toList();
    }
}
