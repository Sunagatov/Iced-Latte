package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.OrderItemResponseDto;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.stub.OrderDtoTestStub;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderItemDtoConverterTest {

    private ProductInfoDtoConverter productInfoDtoConverter = new ProductInfoDtoConverter();

    private OrderItemDtoConverter orderItemDtoConverter = new OrderItemDtoConverter(productInfoDtoConverter);

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
