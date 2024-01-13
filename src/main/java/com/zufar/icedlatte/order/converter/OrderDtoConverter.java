package com.zufar.icedlatte.order.converter;

import com.zufar.icedlatte.openapi.dto.AddedOrder;
import com.zufar.icedlatte.openapi.dto.ListOfAddedOrders;
import com.zufar.icedlatte.openapi.dto.OrderDto;
import com.zufar.icedlatte.order.entity.Order;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = OrderItemDtoConverter.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE, injectionStrategy = InjectionStrategy.FIELD)
public interface OrderDtoConverter {
    @Mapping(target = "items", source = "items")
    Order toOrder(final OrderDto orderDto);

    @Mapping(target = "items", source = "order.items", qualifiedByName = {"toAddedOrderItem"})
    AddedOrder toAddedOrder(final Order order);

    default ListOfAddedOrders toList(final List<AddedOrder> orders){
        var list = new ListOfAddedOrders();
        list.setOrders(orders);
        return list;
    }
}
