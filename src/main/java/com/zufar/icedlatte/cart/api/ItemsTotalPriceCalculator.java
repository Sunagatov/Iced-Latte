package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Set;

@Service
public class ItemsTotalPriceCalculator {

    @Named("toItemsTotalPrice")
    public BigDecimal calculate(Set<ShoppingCartItem> items) {
        return items.stream()
                .map(item -> item.getProductInfo().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
