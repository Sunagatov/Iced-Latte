package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.order.api.OrderPaymentApi;
import com.zufar.icedlatte.order.api.OrderSnapshot;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPaymentFacade implements OrderPaymentApi {

    private final OrderDetailProvider orderDetailProvider;
    private final OrderStatusTransitioner orderStatusTransitioner;
    private final OrderRepository orderRepository;

    @Override
    public OrderSnapshot getSnapshot(UUID orderId) {
        return orderDetailProvider.getSnapshot(orderId);
    }

    @Override
    public OrderSnapshot getSnapshotWithItems(UUID orderId) {
        return orderDetailProvider.getSnapshotWithItems(orderId);
    }

    @Override
    public Optional<OrderSnapshot> findByStripePaymentIntentId(String paymentIntentId) {
        return orderDetailProvider.findSnapshotByStripePaymentIntentId(paymentIntentId);
    }

    @Override
    @Transactional
    public void confirmPayment(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.PENDING_PAYMENT_CONFIRMED, null, reason);
    }

    @Override
    @Transactional
    public void expirePayment(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.PAYMENT_EXPIRED_EVENT, null, reason);
    }

    @Override
    @Transactional
    public void failPayment(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.PAYMENT_FAILED_EVENT, null, reason);
    }

    @Override
    @Transactional
    public void assignPaymentIntent(UUID orderId, String stripePaymentIntentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setStripePaymentIntentId(stripePaymentIntentId);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void confirmRefund(UUID orderId, String reason) {
        orderStatusTransitioner.transition(orderId, OrderEvent.REFUND_CONFIRMED, null, reason);
    }
}
