package com.zufar.icedlatte.product.repository;

import com.zufar.icedlatte.product.entity.ProductInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductSpecifications unit tests")
class ProductSpecificationsTest {

    @Test
    void minPriceSpec_null_returnsNone() {
        assertThat(eval(ProductSpecifications.minPriceSpec(null))).isNull();
    }

    @Test
    void minPriceSpec_nonNull_returnsSpec() {
        assertThat(ProductSpecifications.minPriceSpec(BigDecimal.ONE)).isNotNull();
    }

    @Test
    void maxPriceSpec_null_returnsNone() {
        assertThat(eval(ProductSpecifications.maxPriceSpec(null))).isNull();
    }

    @Test
    void maxPriceSpec_nonNull_returnsSpec() {
        assertThat(ProductSpecifications.maxPriceSpec(BigDecimal.TEN)).isNotNull();
    }

    @Test
    void minRatingSpec_null_returnsNone() {
        assertThat(eval(ProductSpecifications.minRatingSpec(null))).isNull();
    }

    @Test
    void minRatingSpec_nonNull_returnsSpec() {
        assertThat(ProductSpecifications.minRatingSpec(BigDecimal.ONE)).isNotNull();
    }

    @Test
    void brandNamesSpec_null_returnsNone() {
        assertThat(eval(ProductSpecifications.brandNamesSpec(null))).isNull();
    }

    @Test
    void brandNamesSpec_empty_returnsNone() {
        assertThat(eval(ProductSpecifications.brandNamesSpec(List.of()))).isNull();
    }

    @Test
    void brandNamesSpec_nonEmpty_returnsSpec() {
        assertThat(ProductSpecifications.brandNamesSpec(List.of("Brand"))).isNotNull();
    }

    @Test
    void sellerNamesSpec_null_returnsNone() {
        assertThat(eval(ProductSpecifications.sellerNamesSpec(null))).isNull();
    }

    @Test
    void sellerNamesSpec_empty_returnsNone() {
        assertThat(eval(ProductSpecifications.sellerNamesSpec(List.of()))).isNull();
    }

    @Test
    void sellerNamesSpec_nonEmpty_returnsSpec() {
        assertThat(ProductSpecifications.sellerNamesSpec(List.of("Seller"))).isNotNull();
    }

    @Test
    void nameContainsSpec_null_returnsNone() {
        assertThat(eval(ProductSpecifications.nameContainsSpec(null))).isNull();
    }

    @Test
    void nameContainsSpec_blank_returnsNone() {
        assertThat(eval(ProductSpecifications.nameContainsSpec("  "))).isNull();
    }

    @Test
    void nameContainsSpec_nonBlank_returnsSpec() {
        assertThat(ProductSpecifications.nameContainsSpec("latte")).isNotNull();
    }

    private Object eval(Specification<ProductInfo> spec) {
        return spec.toPredicate(null, null, null);
    }
}
