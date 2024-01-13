package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.OrderItemResponseDto;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.stub.OrderDtoTestStub;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {OrderDtoConverterTest.Config.class})
class OrderItemDtoConverterTest {


    @Autowired
    ProductInfoDtoConverter productInfoDtoConverter;
    @Autowired
    OrderItemDtoConverter orderItemDtoConverter;

    @Configuration
    public static class Config {
        @Bean
        public OrderItemDtoConverter orderItemDtoConverter() {
            return Mappers.getMapper(OrderItemDtoConverter.class);
        }
        @Bean
        public ProductInfoDtoConverter productInfoDtoConverter() {
            return Mappers.getMapper(ProductInfoDtoConverter.class);
        }
    }

    @Test
    @DisplayName("toOrderItemResponseDto should convert OrderItem to OrderItemResponseDto with complete information")
    void shouldConvertOrderItemToOrderItemResponseDto() {
        OrderItem orderItem = OrderDtoTestStub.createFirstOrderItem(null);
        OrderItemResponseDto orderItemResponse = orderItemDtoConverter.toDto(orderItem);

        assertEquals(orderItem.getId(), orderItemResponse.getId());
        assertEquals(orderItem.getProductQuantity(), orderItemResponse.getProductQuantity());
        assertEquals(orderItem.getProductInfo().getProductId(), orderItemResponse.getProductInfo().getId());
        assertEquals(orderItem.getProductInfo().getPrice(), orderItemResponse.getProductInfo().getPrice());
        assertEquals(orderItem.getProductInfo().getDescription(), orderItemResponse.getProductInfo().getDescription());
        assertEquals(orderItem.getProductInfo().getQuantity(), orderItemResponse.getProductInfo().getQuantity());
        assertEquals(orderItem.getProductInfo().getName(), orderItemResponse.getProductInfo().getName());
    }
}
