package com.zufar.icedlatte.order.api;


import com.zufar.icedlatte.openapi.dto.OrderResponseDto;
import com.zufar.icedlatte.openapi.dto.OrderStatus;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.createOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProviderTest {

    @InjectMocks
    private OrderProvider orderProvider;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Mock
    private OrderDtoConverter orderDtoConverter;

    @Test
    @DisplayName("getOrdersByStatus should return the OrderResponseDto")
    void shouldReturnListOfOrders() {
        UUID userId = UUID.randomUUID();
        var orderEntity = createOrder();
        var orders = List.of(orderEntity);
        var orderResponseDto = new OrderResponseDto();
        var responseList = List.of(orderResponseDto);
        var statuses = List.of(OrderStatus.CREATED);

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(orderRepository.findAllByUserIdAndStatus(userId, statuses)).thenReturn(orders);
        when(orderDtoConverter.toResponseDto(orderEntity)).thenReturn(orderResponseDto);

        List<OrderResponseDto> result = orderProvider.getOrdersByStatus(statuses);

        assertEquals(result, responseList);

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(orderRepository, times(1)).findAllByUserIdAndStatus(userId, statuses);
        verify(orderDtoConverter, times(1)).toResponseDto(orderEntity);
    }

}
