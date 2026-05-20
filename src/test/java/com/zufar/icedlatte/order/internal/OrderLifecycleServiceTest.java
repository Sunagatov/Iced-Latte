package com.zufar.icedlatte.order.internal;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderEvent;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.exception.OrderAccessDeniedException;
import com.zufar.icedlatte.order.exception.OrderNotFoundException;
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
@DisplayName("OrderLifecycleService unit tests")
class OrderLifecycleServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusTransitioner statusTransitioner;
    @Mock private OrderDtoConverter orderDtoConverter;
    @InjectMocks private OrderLifecycleService lifecycleService;

    @Test
    @DisplayName("Cancel delegates to transitioner with CANCEL event")
    void cancelDelegatesToTransitioner() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(userId).status(OrderStatus.CREATED).build();
        Order cancelled = Order.builder().id(orderId).userId(userId).status(OrderStatus.CANCELLED).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(statusTransitioner.transition(eq(orderId), eq(OrderEvent.CANCEL), eq(userId), any()))
                .thenReturn(cancelled);
        when(orderDtoConverter.toResponseDto(cancelled)).thenReturn(new OrderDto());

        lifecycleService.cancel(orderId, userId);
        verify(statusTransitioner).transition(orderId, OrderEvent.CANCEL, userId, "User cancelled");
    }

    @Test
    @DisplayName("Refund delegates to transitioner with REQUEST_REFUND event")
    void refundDelegatesToTransitioner() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(userId).status(OrderStatus.PAID).build();
        Order refunded = Order.builder().id(orderId).userId(userId).status(OrderStatus.REFUND_REQUESTED).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(statusTransitioner.transition(orderId, OrderEvent.REQUEST_REFUND, userId, "Damaged"))
                .thenReturn(refunded);
        when(orderRepository.save(any())).thenReturn(refunded);
        when(orderDtoConverter.toResponseDto(refunded)).thenReturn(new OrderDto());

        lifecycleService.requestRefund(orderId, userId, "Damaged");
        verify(statusTransitioner).transition(orderId, OrderEvent.REQUEST_REFUND, userId, "Damaged");
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when order does not exist")
    void cancelNotFoundThrows() {
        when(orderRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lifecycleService.cancel(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Throws OrderAccessDeniedException for other user's order")
    void cancelOtherUserThrows() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(UUID.randomUUID()).status(OrderStatus.CREATED).build();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> lifecycleService.cancel(orderId, UUID.randomUUID()))
                .isInstanceOf(OrderAccessDeniedException.class);
    }
}
