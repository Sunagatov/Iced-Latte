package com.zufar.icedlatte.product.validator;

import com.zufar.icedlatte.common.validation.pagination.PaginationParametersValidator;
import com.zufar.icedlatte.product.exception.GetProductsBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("accepts valid request")
        void acceptsValidRequest() {
            assertThatCode(() -> validator.validate(0, 10, "name", "asc",
                    BigDecimal.ONE, BigDecimal.TEN, 3, List.of("Brand"), List.of("Seller")))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rejects negative min price")
        void rejectsNegativeMinPrice() {
            assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                    BigDecimal.valueOf(-1), null, null, null, null))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("minPrice");
        }

        @Test
        @DisplayName("rejects negative max price")
        void rejectsNegativeMaxPrice() {
            assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                    null, BigDecimal.valueOf(-5), null, null, null))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("maxPrice");
        }

        @Test
        @DisplayName("rejects min price greater than max price")
        void rejectsMinPriceGreaterThanMaxPrice() {
            assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                    BigDecimal.TEN, BigDecimal.ONE, null, null, null))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("maxPrice");
        }

        @Test
        @DisplayName("rejects unsupported minimum average rating")
        void rejectsUnsupportedMinimumAverageRating() {
            assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                    null, null, 5, null, null))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("minimumAverageRating");
        }

        @Test
        @DisplayName("accepts valid minimum average rating values 1 through 4")
        void acceptsValidMinimumAverageRatingValues() {
            for (int rating = 1; rating <= 4; rating++) {
                final int current = rating;
                assertThatCode(() -> validator.validate(0, 10, "name", "asc",
                        null, null, current, null, null))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("rejects blank brand name")
        void rejectsBlankBrandName() {
            assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                    null, null, null, List.of("Brand", ""), null))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("brandNames");
        }

        @Test
        @DisplayName("rejects duplicate brand names")
        void rejectsDuplicateBrandNames() {
            assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                    null, null, null, List.of("Brand", "Brand"), null))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("brandNames");
        }

        @Test
        @DisplayName("rejects duplicate seller names")
        void rejectsDuplicateSellerNames() {
            assertThatThrownBy(() -> validator.validate(0, 10, "name", "asc",
                    null, null, null, null, List.of("Seller", "Seller")))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("sellerNames");
        }

        @Test
        @DisplayName("rejects invalid sort attribute")
        void rejectsInvalidSortAttribute() {
            assertThatThrownBy(() -> validator.validate(0, 10, "unknown", "asc",
                    null, null, null, null, null))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("sortAttribute");
        }

        @Test
        @DisplayName("allows null page size because provider applies default")
        void allowsNullPageSizeBecauseProviderAppliesDefault() {
            assertThatCode(() -> validator.validate(0, null, "name", "asc",
                    null, null, null, null, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("aggregates independent validation errors into one exception")
        void aggregatesIndependentValidationErrorsIntoOneException() {
            assertThatThrownBy(() -> validator.validate(-1, 0, "unknown", "asc",
                    BigDecimal.valueOf(-1), BigDecimal.valueOf(-2), 5, List.of(""), List.of("Seller", "Seller")))
                    .isInstanceOf(GetProductsBadRequestException.class)
                    .hasMessageContaining("PageNumber")
                    .hasMessageContaining("PageSize")
                    .hasMessageContaining("sortAttribute")
                    .hasMessageContaining("minPrice")
                    .hasMessageContaining("maxPrice")
                    .hasMessageContaining("minimumAverageRating")
                    .hasMessageContaining("brandNames")
                    .hasMessageContaining("sellerNames");
        }
    }
}
