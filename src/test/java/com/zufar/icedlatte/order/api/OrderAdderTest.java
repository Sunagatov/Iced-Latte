package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.OrderResponseDto;
import com.zufar.icedlatte.order.converter.OrderDtoConverter;
import com.zufar.icedlatte.order.repository.OrderRepository;
import com.zufar.icedlatte.security.api.SecurityPrincipalProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderAdderTest {

    @InjectMocks
    private OrderAdder orderAdder;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Mock
    private OrderCreator orderCreator;

    @Mock
    private OrderDtoConverter orderDtoConverter;

    @Test
    @DisplayName("addOrder should return the OrderResponseDto")
    void shouldAddOrderAndReturnOrderResponseDto() {
        UUID userId = UUID.randomUUID();
        var orderRequest = createOrderRequestDto();
        var orderEntity = createOrder();
        var orderResponse = new OrderResponseDto();

        when(securityPrincipalProvider.getUserId()).thenReturn(userId);
        when(orderCreator.createNewOrder(orderRequest, userId)).thenReturn(orderEntity);
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderDtoConverter.toResponseDto(orderEntity)).thenReturn(orderResponse);

        OrderResponseDto result = orderAdder.addOrder(orderRequest);

        assertEquals(result, orderResponse);

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(orderCreator, times(1)).createNewOrder(orderRequest, userId);
        verify(orderRepository, times(1)).save(orderEntity);
        verify(orderDtoConverter, times(1)).toResponseDto(orderEntity);
    }
}
