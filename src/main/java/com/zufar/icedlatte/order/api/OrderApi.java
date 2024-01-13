package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.*;
import java.util.Set;

public interface OrderApi {

    /**
     * Enables to add a new order into the database for an authorized user
     *
     * @param order which we would like to create for the user
     * @return AddedOrder (the order details)
     */
    AddedOrder addNewOrder(final OrderDto order);

    /**
     * Enables to update the status of order
     *
     * @param orderInfo order id and status to be updated
     * @return AddedOrder (the order details)
     */
    AddedOrder updateOrderStatus(final UpdateOrderStatusDto orderInfo);

    /**
     * Enables to get orders filtered by status for an authorized user
     *
     * @param statusList array of status codes to be applied for orders filtering
     * @return AddedOrder (the order details)
     */
    ListOfAddedOrders getAllOrders(final Set<OrderStatus> statusList);
}
