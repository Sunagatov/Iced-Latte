package com.zufar.icedlatte.order.stub;

import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.openapi.dto.OrderItemDto;
import com.zufar.icedlatte.order.entity.Order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderDtoTestStub {

    public static OrderDto createOrderDto() {
        OrderDto orderDto = new OrderDto();
        orderDto.setDeliveryCost(new BigDecimal("2.58"));
        orderDto.setTaxCost(new BigDecimal("4.76"));
        orderDto.setDeliveryInfo("London");
        orderDto.setRecipientName("Jane");
        orderDto.setRecipientSurname("Doe");
        orderDto.setEmail("jane.doe@random.com");
        orderDto.setPhoneNumber("+3810000000");

        return orderDto;
    }

    public static Order createOrder() {
        return Order.builder()
                .id(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .deliveryCost(new BigDecimal("2.58"))
                .taxCost(new BigDecimal("4.76"))
                .deliveryInfo("London")
                .recipientName("Jane")
                .recipientSurname("Doe")
                .email("jane.doe@random.com")
                .phoneNumber("+3810000000")
                // TODO: add items and other fields
                .build();
    }
}
