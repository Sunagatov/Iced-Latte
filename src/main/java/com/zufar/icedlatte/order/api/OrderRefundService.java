package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
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
public class OrderRefundService {

    private final OrderRepository orderRepository;
    private final OrderStatusTransitioner statusTransitioner;
    private final OrderDtoConverter orderDtoConverter;

    @Transactional
    public OrderDto requestRefund(UUID orderId, UUID userId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.getUserId().equals(userId)) {
            throw new OrderAccessDeniedException(orderId);
        }

        Order refundRequested = statusTransitioner.transition(
                orderId, OrderEvent.REQUEST_REFUND, userId, reason);

        refundRequested.setRefundReason(reason);
        orderRepository.save(refundRequested);

        log.info("order.refund.requested: orderId={}, stripePaymentIntentId={}, reason={}",
                orderId, order.getStripePaymentIntentId(), reason);
        // Stripe Refund API call will be added when Phase 5 is implemented.

        return orderDtoConverter.toResponseDto(refundRequested);
    }
}
