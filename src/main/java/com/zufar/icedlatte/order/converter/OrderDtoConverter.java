package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.OrderResponseDto;
import com.zufar.icedlatte.order.api.OrderItemsCalculator;
import com.zufar.icedlatte.order.entity.Order;
import com.zufar.icedlatte.openapi.dto.OrderRequestDto;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {OrderItemDtoConverter.class, OrderItemsCalculator.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.FIELD)
public interface OrderDtoConverter {

    @Mapping(target = "items", source = "items")
    Order toOrderEntity(final OrderRequestDto orderDto);

    @Mapping(target = "items", source = "order.items", qualifiedByName = {"toOrderItemResponseDto"})
    @Mapping(target = "itemsTotalPrice", source = "order.items", qualifiedByName = {"toItemsTotalPrice"})
    @Mapping(target = "totalOrderCost", expression = "java(OrderItemsCalculator.calculate(orderResponseDto.getItemsTotalPrice(), order.getDeliveryCost(), order.getTaxCost()))")
    OrderResponseDto toResponseDto(final Order order);
}
