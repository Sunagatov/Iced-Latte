package com.zufar.icedlatte.order.api;

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
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final OrderStatusTransitioner statusTransitioner;
    private final OrderDtoConverter orderDtoConverter;

    @Transactional
    public OrderDto cancel(UUID orderId, UUID userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException(orderId);
        }

        boolean wasPaid = order.getStatus() == OrderStatus.PAID;

        Order cancelled = statusTransitioner.transition(orderId, OrderEvent.CANCEL, userId, "User cancelled");

        if (wasPaid) {
            log.warn("order.cancel.refund_needed: orderId={}, stripePaymentIntentId={}. " +
                            "Automatic Stripe refund will be triggered when Stripe integration is active.",
                    orderId, order.getStripePaymentIntentId());
            // TODO: When Stripe refund API is integrated (Phase 5), call it here with @Retryable.
            // On failure after retries, log CRITICAL alert for manual admin intervention.
        }

        return orderDtoConverter.toResponseDto(cancelled);
    }
}
