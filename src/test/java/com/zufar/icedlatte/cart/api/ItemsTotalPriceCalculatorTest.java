package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingSession;
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

    static final BigDecimal TOTAL_PRICE_FOR_SHOPPING_SESSION = BigDecimal.valueOf(15.4);

    @InjectMocks
    private ItemsTotalPriceCalculator itemsTotalPriceCalculator;

    @Test
    @DisplayName("Calculate should return the Total price of items in ShoppingSession")
    void shouldReturnCorrectTotalPrice() {
        ShoppingSession shoppingSession = CartDtoTestStub.createShoppingSession();

        BigDecimal result = itemsTotalPriceCalculator.calculate(shoppingSession.getItems());

        assertEquals(TOTAL_PRICE_FOR_SHOPPING_SESSION, result);
    }
}
