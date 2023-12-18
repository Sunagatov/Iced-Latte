package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCartItem;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class ItemsTotalPriceCalculator {
    public static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.05");
    public static final BigDecimal DEFAULT_SHIPPING_COST = new BigDecimal("5.00");

    @Named("toItemsTotalPrice")
    public BigDecimal calculate(Set<ShoppingCartItem> items) {
        BigDecimal subtotal = items.stream()
                .map(item -> item.getProductInfo().getPrice().multiply(BigDecimal.valueOf(item.getProductQuantity())))
                .map(price -> price.add(price.multiply(DEFAULT_TAX_RATE)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.add(DEFAULT_SHIPPING_COST);
    }
}
