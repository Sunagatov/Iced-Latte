package com.zufar.icedlatte.product.specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.zufar.icedlatte.product.entity.ProductInfo;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProductSpecifications {

    private static final Specification<ProductInfo> NONE = (_, _, _) -> null;

    public static Specification<ProductInfo> minPriceSpec(BigDecimal minPrice) {
        return minPrice == null ? NONE : (r, _, cb) -> cb.greaterThanOrEqualTo(r.get("price"), minPrice);
    }

    public static Specification<ProductInfo> maxPriceSpec(BigDecimal maxPrice) {
        return maxPrice == null ? NONE : (r, _, cb) -> cb.lessThanOrEqualTo(r.get("price"), maxPrice);
    }

    public static Specification<ProductInfo> minRatingSpec(BigDecimal minRating) {
        return minRating == null ? NONE : (r, _, cb) -> cb.greaterThanOrEqualTo(r.get("averageRating"), minRating);
    }

    public static Specification<ProductInfo> brandNamesSpec(List<String> brandNames) {
        return (brandNames == null || brandNames.isEmpty())
                ? NONE
                : (r, _, _) -> r.get("brandName").in(brandNames);
    }

    public static Specification<ProductInfo> sellerNamesSpec(List<String> sellerNames) {
        return (sellerNames == null || sellerNames.isEmpty())
                ? NONE
                : (r, _, _) -> r.get("sellerName").in(sellerNames);
    }

    public static Specification<ProductInfo> nameContainsSpec(String keyword) {
        if (keyword == null || keyword.isBlank()) return NONE;
        String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT).trim()) + "%";
        return (r, _, cb) -> cb.like(cb.lower(r.get("name")), pattern, '\\');
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
