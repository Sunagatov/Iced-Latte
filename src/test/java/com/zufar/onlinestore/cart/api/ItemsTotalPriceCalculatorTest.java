package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.entity.ShoppingSessionItem;
import com.zufar.onlinestore.product.entity.ProductInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
class ItemsTotalPriceCalculatorTest {

    @InjectMocks
    private ItemsTotalPriceCalculator itemsTotalPriceCalculator;

    @Test
    void shouldReturnCorrectTotalPrice() {
        ProductInfo productInfo = new ProductInfo();
        productInfo.setPrice(BigDecimal.ONE);

        ShoppingSessionItem item = new ShoppingSessionItem();
        item.setProductInfo(productInfo);
        item.setProductQuantity(5);

        Set<ShoppingSessionItem> items = new HashSet<>();
        items.add(item);

        BigDecimal result = itemsTotalPriceCalculator.calculate(items);
        BigDecimal expected = BigDecimal.valueOf(5);

        assertEquals(result, expected);
    }
}
