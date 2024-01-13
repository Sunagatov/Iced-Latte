package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.order.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public final class Calculator {

    public static BigDecimal calculate(Set<OrderItem> items) {
        return items.stream()
                .map(item -> item.getProductInfo().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal calculate(BigDecimal totalProductsCost, BigDecimal deliveryCost, BigDecimal taxCost) {
        return totalProductsCost.add(deliveryCost).add(taxCost);
    }

    private Calculator() {}
}
