package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;

@Slf4j
@Service
public class ItemsTotalPriceCalculator {

    public BigDecimal calculate(Collection<ShoppingSessionItemDto> items) {
        return items.stream()
                .map(item -> item.productInfo().price()
                        .multiply(BigDecimal.valueOf(item.productsQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
