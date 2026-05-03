package com.zufar.icedlatte.order.validator;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderCancellationWindowExpiredException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class OrderStatusTransitionValidator {

    public void validate(Order order, OrderEvent event, UUID actorId) {
        if (event == OrderEvent.CANCEL) {
            validateCancellationWindow(order);
        }
        if (event == OrderEvent.REQUEST_REFUND) {
            validateOwnership(order, actorId);
        }
    }

    private void validateCancellationWindow(Order order) {
        if (order.getCancellationDeadline() != null
                && OffsetDateTime.now().isAfter(order.getCancellationDeadline())) {
            throw new OrderCancellationWindowExpiredException(order.getId());
        }
    }

    private void validateOwnership(Order order, UUID actorId) {
        if (!order.getUserId().equals(actorId)) {
            throw new OrderAccessDeniedException(order.getId());
        }
    }
}
