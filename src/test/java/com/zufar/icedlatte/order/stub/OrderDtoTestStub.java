package com.zufar.icedlatte.order.stub;

import com.zufar.icedlatte.openapi.dto.OrderItemRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderRequestDto;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import com.zufar.icedlatte.product.entity.ProductInfo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class OrderDtoTestStub {


    public static final BigDecimal EXPECTED_ITEMS_TOTAL_PRICE = BigDecimal.valueOf(12.1);

    public static final BigDecimal EXPECTED_ORDER_TOTAL_COST = BigDecimal.valueOf(20.18);

    public static final BigDecimal TAX_COST = BigDecimal.valueOf(2.48);
    public static final BigDecimal DELIVERY_COST = BigDecimal.valueOf(5.6);

    public static final Integer EXPECTED_ITEMS_QUANTITY = 8;

    public static OrderRequestDto createOrderRequestDto() {
        OrderRequestDto orderDto = new OrderRequestDto();
        orderDto.setDeliveryCost(DELIVERY_COST);
        orderDto.setTaxCost(TAX_COST);
        orderDto.setDeliveryInfo("London");
        orderDto.setRecipientName("Jane");
        orderDto.setRecipientSurname("Doe");
        orderDto.setEmail("jane.doe@random.com");
        orderDto.setPhoneNumber("+3810000000");
        orderDto.addItemsItem(createRandomOrderItemRequestDto());
        orderDto.addItemsItem(createRandomOrderItemRequestDto());
        return orderDto;
    }

    public static OrderItemRequestDto createRandomOrderItemRequestDto() {
        var item = new OrderItemRequestDto();
        item.setProductId(UUID.randomUUID());
        item.setProductQuantity((int) (Math.random() * 10));
        return item;
    }

    public static Order createOrder() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .deliveryCost(DELIVERY_COST)
                .taxCost(TAX_COST)
                .deliveryInfo("London")
                .recipientName("Jane")
                .recipientSurname("Doe")
                .email("jane.doe@random.com")
                .phoneNumber("+3810000000")
                .build();

        OrderItem firstItem = createFirstOrderItem(order);
        OrderItem secondItem = createSecondOrderItem(order);
        var items = List.of(firstItem, secondItem);
        order.setItems(items);
        order.setItemsQuantity(8);
        return order;
    }

    public static OrderItem createFirstOrderItem(Order order) {
        UUID firstProductId = UUID.fromString("a834c24e-886d-470f-bf19-7454a60f0639");
        ProductInfo firstProductInfo = new ProductInfo(
                firstProductId, "First test name", "First test description", BigDecimal.valueOf(1.1), 30, true);
        return new OrderItem(firstProductId, order, firstProductInfo, 5);
    }

    public static OrderItem createSecondOrderItem(Order order) {
        UUID secondProductId = UUID.fromString("2ade78e3-aa45-4b6b-adf4-86f8302ced7d");
        ProductInfo secondProductInfo = new ProductInfo(
                secondProductId, "Second test name", "Second test description", BigDecimal.valueOf(2.2), 70, true);
        return new OrderItem(secondProductId, order, secondProductInfo, 3);
    }
}
