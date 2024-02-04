package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.OrderItemRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderItemResponseDto;
import com.zufar.icedlatte.openapi.dto.OrderRequestDto;
import com.zufar.icedlatte.openapi.dto.OrderResponseDto;
import com.zufar.icedlatte.order.api.OrderItemsCalculator;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDtoConverter {

    private final OrderItemDtoConverter orderItemDtoConverter;

    public Order toOrderEntity(final OrderRequestDto orderDto) {
        if (orderDto == null) {
            return null;
        }

        Order.OrderBuilder order = Order.builder();

        order.items(orderItemRequestDtoListToOrderItemList(orderDto.getItems()));
        order.deliveryCost(orderDto.getDeliveryCost());
        order.taxCost(orderDto.getTaxCost());
        order.deliveryInfo(orderDto.getDeliveryInfo());
        order.recipientName(orderDto.getRecipientName());
        order.recipientSurname(orderDto.getRecipientSurname());
        order.email(orderDto.getEmail());
        order.phoneNumber(orderDto.getPhoneNumber());

        return order.build();
    }

    public OrderResponseDto toResponseDto(final Order order) {
        if (order == null) {
            return null;
        }

        OrderResponseDto orderResponseDto = new OrderResponseDto();

        orderResponseDto.setItems(orderItemListToOrderItemResponseDtoList(order.getItems()));
        orderResponseDto.setItemsTotalPrice(OrderItemsCalculator.calculate(order.getItems()));
        orderResponseDto.setId(order.getId());
        orderResponseDto.setUserId(order.getUserId());
        orderResponseDto.setStatus(order.getStatus());
        orderResponseDto.setItemsQuantity(order.getItemsQuantity());
        orderResponseDto.setDeliveryCost(order.getDeliveryCost());
        orderResponseDto.setTaxCost(order.getTaxCost());
        orderResponseDto.setDeliveryInfo(order.getDeliveryInfo());
        orderResponseDto.setRecipientName(order.getRecipientName());
        orderResponseDto.setRecipientSurname(order.getRecipientSurname());
        orderResponseDto.setEmail(order.getEmail());
        orderResponseDto.setPhoneNumber(order.getPhoneNumber());
        orderResponseDto.setCreatedAt(order.getCreatedAt());

        orderResponseDto.setTotalOrderCost(OrderItemsCalculator.calculate(orderResponseDto.getItemsTotalPrice(), order.getDeliveryCost(), order.getTaxCost()));

        return orderResponseDto;
    }

    protected OrderItem orderItemRequestDtoToOrderItem(OrderItemRequestDto orderItemRequestDto) {
        if (orderItemRequestDto == null) {
            return null;
        }

        OrderItem.OrderItemBuilder orderItem = OrderItem.builder();

        orderItem.productQuantity(orderItemRequestDto.getProductQuantity());

        return orderItem.build();
    }

    protected List<OrderItem> orderItemRequestDtoListToOrderItemList(List<OrderItemRequestDto> list) {
        if (list == null) {
            return null;
        }

        List<OrderItem> list1 = new ArrayList<OrderItem>(list.size());
        for (OrderItemRequestDto orderItemRequestDto : list) {
            list1.add(orderItemRequestDtoToOrderItem(orderItemRequestDto));
        }

        return list1;
    }

    protected List<OrderItemResponseDto> orderItemListToOrderItemResponseDtoList(List<OrderItem> list) {
        if (list == null) {
            return null;
        }

        List<OrderItemResponseDto> list1 = new ArrayList<OrderItemResponseDto>(list.size());
        for (OrderItem orderItem : list) {
            list1.add(orderItemDtoConverter.toDto(orderItem));
        }

        return list1;
    }
}
