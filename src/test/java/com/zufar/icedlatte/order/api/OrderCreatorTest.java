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

import java.util.UUID;

import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.createOrder;
import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.createOrderRequestDto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreatorTest {

    @InjectMocks
    private OrderCreator orderCreator;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SecurityPrincipalProvider securityPrincipalProvider;

    @Mock
    private OrderEntityCreator orderEntityCreator;

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
        when(orderEntityCreator.createNewOrder(orderRequest, userId)).thenReturn(orderEntity);
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderDtoConverter.toResponseDto(orderEntity)).thenReturn(orderResponse);

        OrderResponseDto result = orderCreator.createNewOrder(orderRequest);

        assertEquals(result, orderResponse);

        verify(securityPrincipalProvider, times(1)).getUserId();
        verify(orderEntityCreator, times(1)).createNewOrder(orderRequest, userId);
        verify(orderRepository, times(1)).save(orderEntity);
        verify(orderDtoConverter, times(1)).toResponseDto(orderEntity);
    }
}
