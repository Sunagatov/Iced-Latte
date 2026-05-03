package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.exception.InvalidOrderStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static com.zufar.icedlatte.openapi.dto.OrderEvent.*;
import static com.zufar.icedlatte.openapi.dto.OrderStatus.*;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Map<OrderEvent, OrderStatus>> TRANSITIONS = Map.of(
            CREATED, Map.of(PAYMENT_CONFIRMED, PAID, CANCEL, CANCELLED),
            PAID, Map.of(SHIP, SHIPPED, CANCEL, CANCELLED, REQUEST_REFUND, REFUND_REQUESTED),
            SHIPPED, Map.of(DELIVER, DELIVERED),
            REFUND_REQUESTED, Map.of(REFUND_CONFIRMED, REFUNDED)
    );

    public OrderStatus transition(OrderStatus current, OrderEvent event) {
        return Optional.ofNullable(TRANSITIONS.get(current))
                .map(events -> events.get(event))
                .orElseThrow(() -> new InvalidOrderStateTransitionException(current, event));
    }
}
