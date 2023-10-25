package com.zufar.onlinestore.cart.api;

import com.zufar.onlinestore.cart.entity.ShoppingSession;
import com.zufar.onlinestore.cart.stub.CartDtoTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ItemsTotalPriceCalculatorTest {

    @InjectMocks
    private ItemsTotalPriceCalculator itemsTotalPriceCalculator;

    @Test
    @DisplayName("calculate should return the Total price of items in ShoppingSession")
    public void calculate_shouldReturnCorrectTotalPrice() {
        ShoppingSession shoppingSession = CartDtoTestUtil.createShoppingSession();

        BigDecimal result = itemsTotalPriceCalculator.calculate(shoppingSession.getItems());

        assertEquals(BigDecimal.valueOf(15.4), result);
    }
}
