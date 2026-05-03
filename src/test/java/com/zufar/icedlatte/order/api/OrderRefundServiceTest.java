package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRefundService unit tests")
class OrderRefundServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusTransitioner statusTransitioner;
    @Mock private OrderDtoConverter orderDtoConverter;
    @InjectMocks private OrderRefundService refundService;

    @Test
    @DisplayName("Requests refund for a PAID order with reason")
    void requestRefundForPaidOrder() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(userId).status(OrderStatus.PAID).build();
        Order refundRequested = Order.builder().id(orderId).userId(userId).status(OrderStatus.REFUND_REQUESTED).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(statusTransitioner.transition(eq(orderId), eq(OrderEvent.REQUEST_REFUND), eq(userId), eq("Damaged")))
                .thenReturn(refundRequested);
        when(orderRepository.save(any())).thenReturn(refundRequested);
        when(orderDtoConverter.toResponseDto(refundRequested)).thenReturn(new OrderDto());

        refundService.requestRefund(orderId, userId, "Damaged");

        verify(statusTransitioner).transition(orderId, OrderEvent.REQUEST_REFUND, userId, "Damaged");
        verify(orderRepository).save(refundRequested);
    }

    @Test
    @DisplayName("Throws when user doesn't own the order")
    void requestRefundOtherUsersOrderThrows() {
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(ownerId).status(OrderStatus.PAID).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> refundService.requestRefund(orderId, UUID.randomUUID(), null))
                .isInstanceOf(OrderAccessDeniedException.class);
    }
}
