package com.zufar.icedlatte.cart.api;

import com.zufar.icedlatte.cart.entity.ShoppingCart;
import com.zufar.icedlatte.cart.stub.CartDtoTestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemsTotalPriceCalculator unit tests")
class ItemsTotalPriceCalculatorTest {

    private static final BigDecimal TOTAL_PRICE_FOR_SHOPPING_SESSION = new BigDecimal("15.4");

    @InjectMocks
    private ItemsTotalPriceCalculator itemsTotalPriceCalculator;

    @Nested
    @DisplayName("calculate")
    class Calculate {

        @Test
        @DisplayName("returns total price of all shopping cart items")
        void returnsTotalPriceOfAllShoppingCartItems() {
            ShoppingCart shoppingCart = CartDtoTestStub.createShoppingCart();

            BigDecimal result = itemsTotalPriceCalculator.calculate(shoppingCart.getItems());

            assertThat(result).isEqualByComparingTo(TOTAL_PRICE_FOR_SHOPPING_SESSION);
        }

        @Test
        @DisplayName("returns zero for empty item set")
        void returnsZeroForEmptyItemSet() {
            BigDecimal result = itemsTotalPriceCalculator.calculate(Set.of());

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
