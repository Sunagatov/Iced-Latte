package com.zufar.icedlatte.product.repository;

import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductSpecifications unit tests")
class ProductSpecificationsTest {

    @Nested
    @DisplayName("null and empty guards")
    class NullAndEmptyGuards {

        @Test
        @DisplayName("returns the no-op specification for null numeric filters")
        void returnsNoOpSpecificationForNullNumericFilters() {
            assertThat(eval(ProductSpecifications.minPriceSpec(null))).isNull();
            assertThat(eval(ProductSpecifications.maxPriceSpec(null))).isNull();
            assertThat(eval(ProductSpecifications.minRatingSpec(null))).isNull();
        }

        @Test
        @DisplayName("returns the no-op specification for null or empty list filters")
        void returnsNoOpSpecificationForNullOrEmptyListFilters() {
            assertThat(eval(ProductSpecifications.brandNamesSpec(null))).isNull();
            assertThat(eval(ProductSpecifications.brandNamesSpec(List.of()))).isNull();
            assertThat(eval(ProductSpecifications.sellerNamesSpec(null))).isNull();
            assertThat(eval(ProductSpecifications.sellerNamesSpec(List.of()))).isNull();
        }

        @Test
        @DisplayName("returns the no-op specification for null or blank keyword filters")
        void returnsNoOpSpecificationForNullOrBlankKeywordFilters() {
            assertThat(eval(ProductSpecifications.nameContainsSpec(null))).isNull();
            assertThat(eval(ProductSpecifications.nameContainsSpec("  "))).isNull();
        }
    }

    @Nested
    @DisplayName("active filters")
    class ActiveFilters {

        @Test
        @DisplayName("creates active specifications for populated numeric filters")
        void createsActiveSpecificationsForPopulatedNumericFilters() {
            assertThat(ProductSpecifications.minPriceSpec(BigDecimal.ONE)).isNotNull();
            assertThat(ProductSpecifications.maxPriceSpec(BigDecimal.TEN)).isNotNull();
            assertThat(ProductSpecifications.minRatingSpec(BigDecimal.valueOf(4.5))).isNotNull();
        }

        @Test
        @DisplayName("creates active specifications for populated list filters")
        void createsActiveSpecificationsForPopulatedListFilters() {
            assertThat(ProductSpecifications.brandNamesSpec(List.of("Brand"))).isNotNull();
            assertThat(ProductSpecifications.sellerNamesSpec(List.of("Seller"))).isNotNull();
        }

        @Test
        @DisplayName("creates an active specification for a non-blank keyword")
        void createsActiveSpecificationForNonBlankKeyword() {
            assertThat(ProductSpecifications.nameContainsSpec("latte")).isNotNull();
        }
    }

    private Object eval(Specification<ProductInfo> specification) {
        return specification.toPredicate(null, null, null);
    }
}
