package com.zufar.icedlatte.product.validator;

import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetProductsRequestValidator unit tests")
class GetProductsRequestValidatorTest {

    private GetProductsRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new GetProductsRequestValidator(new PaginationParametersValidator());
    }

    @Test
    @DisplayName("Valid request passes without exception")
    void validate_allValid_noException() {
        assertThatCode(() -> validator.validate(0, 10, "name", "asc",
                BigDecimal.ONE, BigDecimal.TEN, 3, List.of("Brand"), List.of("Seller")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Negative minPrice throws")
    void validate_negativeMinPrice_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                BigDecimal.valueOf(-1), null, null, null, null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("minPrice");
    }

    @Test
    @DisplayName("Negative maxPrice throws")
    void validate_negativeMaxPrice_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                null, BigDecimal.valueOf(-5), null, null, null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("maxPrice");
    }

    @Test
    @DisplayName("minPrice > maxPrice throws")
    void validate_minPriceGreaterThanMaxPrice_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                BigDecimal.TEN, BigDecimal.ONE, null, null, null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("maxPrice");
    }

    @Test
    @DisplayName("Invalid minimumAverageRating throws")
    void validate_invalidMinimumAverageRating_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                null, null, 5, null, null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("minimumAverageRating");
    }

    @Test
    @DisplayName("Valid minimumAverageRating values 1-4 pass")
    void validate_validMinimumAverageRating_noException() {
        for (int rating = 1; rating <= 4; rating++) {
            final int r = rating;
            assertThatCode(() -> validator.validate(0, 10, "name", "asc",
                    null, null, r, null, null))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Blank brandName in list throws")
    void validate_blankBrandName_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                null, null, null, List.of("Brand", ""), null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("brandNames");
    }

    @Test
    @DisplayName("Duplicate brandNames throws")
    void validate_duplicateBrandNames_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                null, null, null, List.of("Brand", "Brand"), null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("brandNames");
    }

    @Test
    @DisplayName("Duplicate sellerNames throws")
    void validate_duplicateSellerNames_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                null, null, null, null, List.of("Seller", "Seller")))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("sellerNames");
    }

    @Test
    @DisplayName("Invalid sortAttribute throws")
    void validate_invalidSortAttribute_throws() {
        assertThatThrownBy(() -> validator.validate(0, 10, "unknown", "asc",
                null, null, null, null, null))
                .isInstanceOf(GetProductsBadRequestException.class)
                .hasMessageContaining("sortAttribute");
    }

    @Test
    @DisplayName("Null optional params all pass")
    void validate_allNullOptionals_noException() {
        assertThatCode(() -> validator.validate(0, 10, "price", "desc",
                null, null, null, null, null))
                .doesNotThrowAnyException();
    }
}
