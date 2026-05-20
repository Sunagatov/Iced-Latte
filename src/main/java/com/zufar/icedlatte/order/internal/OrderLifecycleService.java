package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final OrderStatusTransitioner statusTransitioner;
    private final OrderDtoConverter orderDtoConverter;

    @Transactional
    public OrderDto cancel(UUID orderId, UUID userId) {
        Order order = findOwnedOrder(orderId, userId);
        boolean wasPaid = order.getStatus() == OrderStatus.PAID;

        Order cancelled = statusTransitioner.transition(orderId, OrderEvent.CANCEL, userId, "User cancelled");

        if (wasPaid) {
            log.warn("order.cancel.refund_needed: orderId={}, stripePaymentIntentId={}",
                    orderId, order.getStripePaymentIntentId());
        }

        return orderDtoConverter.toResponseDto(cancelled);
    }

    @Transactional
    public OrderDto requestRefund(UUID orderId, UUID userId, String reason) {
        findOwnedOrder(orderId, userId);

        Order refundRequested = statusTransitioner.transition(
                orderId, OrderEvent.REQUEST_REFUND, userId, reason);

        refundRequested.setRefundReason(reason);
        orderRepository.save(refundRequested);

        log.info("order.refund.requested: orderId={}, stripePaymentIntentId={}",
                orderId, refundRequested.getStripePaymentIntentId());

        return orderDtoConverter.toResponseDto(refundRequested);
    }

    private Order findOwnedOrder(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException();
        }
        return order;
    }
}
