package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.AddedOrder;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.stub.OrderDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.createOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {OrderDtoConverterTest.Config.class})
class OrderDtoConverterTest {

    @Autowired
    OrderDtoConverter orderDtoConverter;

    @Configuration
    public static class Config {

        @Bean
        public OrderDtoConverter orderDtoConverter() {
            return Mappers.getMapper(OrderDtoConverter.class);
        }
    }

    @Test
    @DisplayName("toOrder should convert OrderDto to Order with complete information")
    void shouldConvertOrderDtoToOrder() {
        OrderDto orderDto = OrderDtoTestStub.createOrderDto();
        Order order = orderDtoConverter.toOrder(orderDto);

        assertEquals(orderDto.getDeliveryCost(), order.getDeliveryCost());
        assertEquals(orderDto.getTaxCost(), order.getTaxCost());
        assertEquals(orderDto.getDeliveryInfo(), order.getDeliveryInfo());
        assertEquals(orderDto.getRecipientName(), order.getRecipientName());
        assertEquals(orderDto.getRecipientSurname(), order.getRecipientSurname());
        assertEquals(orderDto.getEmail(), order.getEmail());
        assertEquals(orderDto.getPhoneNumber(), order.getPhoneNumber());
    }

    @Test
    @DisplayName("toAddedOrder should convert Order to AddedOrder with complete information")
    void shouldConvertOrderToAddedOrder() {
        Order order = OrderDtoTestStub.createOrder();
        AddedOrder addedOrder = orderDtoConverter.toAddedOrder(order);
        // TODO: add other fields
        assertEquals(order.getId(), addedOrder.getId());
    }
}
