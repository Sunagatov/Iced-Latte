package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final OrderAdder orderAdder;
    private final OrderProvider orderProvider;

    public OrderResponseDto addOrder(final OrderRequestDto request) {
        return orderAdder.addOrder(request);
    }

    public List<OrderResponseDto> getOrders(final List<OrderStatus> statusList) {
        return orderProvider.getOrdersByStatus(statusList);
    }
}
