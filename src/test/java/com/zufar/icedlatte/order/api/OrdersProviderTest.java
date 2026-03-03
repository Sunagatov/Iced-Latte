package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrdersProvider unit tests")
class OrdersProviderTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderDtoConverter orderDtoConverter;
    @InjectMocks
    private OrdersProvider ordersProvider;

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Returns all orders when statuses list is null")
    void getOrders_nullStatuses_returnsAll() {
        Order order = new Order();
        OrderDto dto = new OrderDto();
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of(order));
        when(orderDtoConverter.toResponseDto(order)).thenReturn(dto);

        List<OrderDto> result = ordersProvider.getOrders(userId, null);

        assertThat(result).containsExactly(dto);
        verify(orderRepository).findAllByUserId(userId);
        verify(orderRepository, never()).findAllByUserIdAndStatusIn(any(), any());
    }

    @Test
    @DisplayName("Returns all orders when statuses list is empty")
    void getOrders_emptyStatuses_returnsAll() {
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<OrderDto> result = ordersProvider.getOrders(userId, List.of());

        assertThat(result).isEmpty();
        verify(orderRepository).findAllByUserId(userId);
    }

    @Test
    @DisplayName("Filters by status when statuses list is provided")
    void getOrders_withStatuses_filtersCorrectly() {
        Order order = new Order();
        OrderDto dto = new OrderDto();
        List<OrderStatus> statuses = List.of(OrderStatus.CREATED);
        when(orderRepository.findAllByUserIdAndStatusIn(userId, statuses)).thenReturn(List.of(order));
        when(orderDtoConverter.toResponseDto(order)).thenReturn(dto);

        List<OrderDto> result = ordersProvider.getOrders(userId, statuses);

        assertThat(result).containsExactly(dto);
        verify(orderRepository).findAllByUserIdAndStatusIn(userId, statuses);
        verify(orderRepository, never()).findAllByUserId(any());
    }

    @Test
    @DisplayName("Returns empty list when no orders found for user")
    void getOrders_noOrders_returnsEmpty() {
        when(orderRepository.findAllByUserId(userId)).thenReturn(List.of());

        List<OrderDto> result = ordersProvider.getOrders(userId, null);

        assertThat(result).isEmpty();
    }
}
