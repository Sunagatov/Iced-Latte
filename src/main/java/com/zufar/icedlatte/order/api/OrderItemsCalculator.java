package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.order.entity.OrderItem;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public final class OrderItemsCalculator {

    @Named("toItemsTotalPrice")
    public static BigDecimal calculate(List<OrderItem> items) {
        return items.stream()
                .map(item -> item
                        .getProductInfo().getPrice()
                        .multiply(BigDecimal.valueOf(item.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Named("toTotalOrderCost")
    public static BigDecimal calculate(BigDecimal totalProductsCost, BigDecimal deliveryCost, BigDecimal taxCost) {
        return totalProductsCost
                .add(deliveryCost)
                .add(taxCost);
    }

    @Named("toTotalItems")
    public static Integer calculate(List<OrderItem> items, Integer defaultValue) {
        return items.stream()
                .map(OrderItem::getProductQuantity)
                .reduce(Integer::sum)
                .orElse(defaultValue);
    }

    private OrderItemsCalculator() {
    }
}
