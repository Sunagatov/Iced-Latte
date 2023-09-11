package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Set;

@Service
public class ItemsTotalPriceCalculator {

    @Named("toItemsTotalPrice")
    public BigDecimal calculate(Set<ShoppingSessionItem> items) {
        return items.stream()
                .map(item -> item.getProductInfo().getPrice().multiply(BigDecimal.valueOf(item.getProductsQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
