package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManager implements OrderApi {

    private final OrderProvider orderProvider;
    private final OrderUpdater orderUpdater;

    @Override
    public AddedOrder addNewOrder(final OrderDto order) {
        return orderProvider.addNewOrder(order);
    }

    @Override
    public ListOfAddedOrders getAllOrders(final Set<OrderStatus> statusList) {
        return orderProvider.getOrdersByStatus(statusList);
    }

    @Override
    public AddedOrder updateOrderStatus(final UpdateOrderStatusDto orderInfo) {
        return orderUpdater.updateStatus(orderInfo);
    }
}
