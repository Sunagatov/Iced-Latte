package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.dto.ShoppingSessionItemDto;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ItemsTotalPriceCalculator {

    public BigDecimal calculate(List<ShoppingSessionItemDto> items) {
        return items.stream()
                .map(item -> item.productInfo().price()
                        .multiply(BigDecimal.valueOf(item.productsQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
