package com.zufar.icedlatte.order.api;

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
@DisplayName("OrderCancellationService unit tests")
class OrderCancellationServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusTransitioner statusTransitioner;
    @Mock private OrderDtoConverter orderDtoConverter;
    @InjectMocks private OrderCancellationService cancellationService;

    @Test
    @DisplayName("Cancels a CREATED order owned by the user")
    void cancelCreatedOrder() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(userId).status(OrderStatus.CREATED).build();
        Order cancelled = Order.builder().id(orderId).userId(userId).status(OrderStatus.CANCELLED).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(statusTransitioner.transition(eq(orderId), eq(OrderEvent.CANCEL), eq(userId), any()))
                .thenReturn(cancelled);
        when(orderDtoConverter.toResponseDto(cancelled)).thenReturn(new OrderDto());

        cancellationService.cancel(orderId, userId);

        verify(statusTransitioner).transition(eq(orderId), eq(OrderEvent.CANCEL), eq(userId), eq("User cancelled"));
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when order does not exist")
    void cancelNonExistentOrderThrows() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancellationService.cancel(orderId, UUID.randomUUID()))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Throws OrderAccessDeniedException when user doesn't own the order")
    void cancelOtherUsersOrderThrows() {
        UUID orderId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Order order = Order.builder().id(orderId).userId(ownerId).status(OrderStatus.CREATED).build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> cancellationService.cancel(orderId, otherUserId))
                .isInstanceOf(OrderAccessDeniedException.class);
    }
}
