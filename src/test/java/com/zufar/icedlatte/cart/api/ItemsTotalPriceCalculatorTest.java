package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ItemsTotalPriceCalculatorTest {

    static final BigDecimal TOTAL_PRICE_FOR_SHOPPING_SESSION = new BigDecimal("15.4");

    @InjectMocks
    private ItemsTotalPriceCalculator itemsTotalPriceCalculator;

    @Test
    @DisplayName("Calculate should return the Total price of items in ShoppingCart")
    void shouldReturnCorrectTotalPrice() {
        ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();

        BigDecimal result = itemsTotalPriceCalculator.calculate(shoppingCart.getItems());

        assertEquals(TOTAL_PRICE_FOR_SHOPPING_SESSION, result);
    }
}
