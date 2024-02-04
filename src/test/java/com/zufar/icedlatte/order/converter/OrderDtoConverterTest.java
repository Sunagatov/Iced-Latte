package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.OrderItemResponseDto;
import com.zufar.icedlatte.openapi.dto.OrderResponseDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.order.stub.OrderDtoTestStub;
import com.zufar.icedlatte.product.converter.ProductInfoDtoConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.EXPECTED_ITEMS_QUANTITY;
import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.EXPECTED_ITEMS_TOTAL_PRICE;
import static com.zufar.icedlatte.order.stub.OrderDtoTestStub.EXPECTED_ORDER_TOTAL_COST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OrderDtoConverterTest {


    private ProductInfoDtoConverter productInfoDtoConverter = new ProductInfoDtoConverter();

    private OrderItemDtoConverter orderItemDtoConverter = new OrderItemDtoConverter(productInfoDtoConverter);

    private OrderDtoConverter orderDtoConverter = new OrderDtoConverter(orderItemDtoConverter);


    @Test
    @DisplayName("toOrderEntity should convert OrderRequestDto to Order entity with complete information")
    void shouldConvertOrderRequestDtoToOrderEntity() {
        var orderRequestDto = OrderDtoTestStub.createOrderRequestDto();
        Order order = orderDtoConverter.toOrderEntity(orderRequestDto);

        assertEquals(orderRequestDto.getDeliveryCost(), order.getDeliveryCost());
        assertEquals(orderRequestDto.getTaxCost(), order.getTaxCost());
        assertEquals(orderRequestDto.getDeliveryInfo(), order.getDeliveryInfo());
        assertEquals(orderRequestDto.getRecipientName(), order.getRecipientName());
        assertEquals(orderRequestDto.getRecipientSurname(), order.getRecipientSurname());
        assertEquals(orderRequestDto.getEmail(), order.getEmail());
        assertEquals(orderRequestDto.getPhoneNumber(), order.getPhoneNumber());
        assertEquals(orderRequestDto.getItems().size(), order.getItems().size());
        for (int i = 0; i < orderRequestDto.getItems().size(); i++) {
            var orderItemDto = orderRequestDto.getItems().get(0);
            var orderItem = order.getItems().get(0);
            assertInstanceOf(OrderItem.class, orderItem);
            assertEquals(orderItemDto.getProductQuantity(), orderItem.getProductQuantity());
        }
    }

    @Test
    @DisplayName("toResponseDto should convert Order entity to OrderResponseDto with complete information")
    void shouldConvertOrderToAddedOrder() {
        Order order = OrderDtoTestStub.createOrder();
        OrderResponseDto response = orderDtoConverter.toResponseDto(order);
        assertEquals(order.getId(), response.getId());
        assertEquals(order.getUserId(), response.getUserId());
        assertEquals(order.getStatus(), response.getStatus());
        assertEquals(order.getItemsQuantity(), response.getItemsQuantity());
        assertEquals(EXPECTED_ITEMS_QUANTITY, response.getItemsQuantity());
        assertEquals(order.getCreatedAt(), response.getCreatedAt());
        assertEquals(order.getDeliveryCost(), response.getDeliveryCost());
        assertEquals(order.getTaxCost(), response.getTaxCost());
        assertEquals(order.getDeliveryInfo(), response.getDeliveryInfo());
        assertEquals(order.getRecipientName(), response.getRecipientName());
        assertEquals(order.getRecipientSurname(), response.getRecipientSurname());
        assertEquals(order.getEmail(), response.getEmail());
        assertEquals(order.getPhoneNumber(), response.getPhoneNumber());
        assertEquals(EXPECTED_ITEMS_TOTAL_PRICE, response.getItemsTotalPrice());
        assertEquals(EXPECTED_ORDER_TOTAL_COST, response.getTotalOrderCost());
        assertEquals(order.getItems().size(), response.getItems().size());
        for (int i = 0; i < order.getItems().size(); i++) {
            var orderItemResponse = response.getItems().get(0);
            var orderItem = order.getItems().get(0);
            assertInstanceOf(OrderItemResponseDto.class, orderItemResponse);
            assertEquals(orderItem.getId(), orderItemResponse.getId());
            assertEquals(orderItem.getProductQuantity(), orderItemResponse.getProductQuantity());
            assertEquals(orderItem.getProductInfo().getProductId(), orderItemResponse.getProductInfo().getId());
            assertEquals(orderItem.getProductInfo().getPrice(), orderItemResponse.getProductInfo().getPrice());
            assertEquals(orderItem.getProductInfo().getDescription(), orderItemResponse.getProductInfo().getDescription());
            assertEquals(orderItem.getProductInfo().getQuantity(), orderItemResponse.getProductInfo().getQuantity());
            assertEquals(orderItem.getProductInfo().getName(), orderItemResponse.getProductInfo().getName());
        }
    }
}
