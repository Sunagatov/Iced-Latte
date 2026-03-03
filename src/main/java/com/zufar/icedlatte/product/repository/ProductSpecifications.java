package com.zufar.icedlatte.product.repository;

import com.zufar.icedlatte.product.entity.ProductInfo;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

public class ProductSpecifications {

    private static final Specification<ProductInfo> NONE = (r, q, cb) -> null;

    public static Specification<ProductInfo> minPriceSpec(BigDecimal minPrice) {
        return minPrice == null ? NONE : (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("price"), minPrice);
    }

    public static Specification<ProductInfo> maxPriceSpec(BigDecimal maxPrice) {
        return maxPrice == null ? NONE : (r, q, cb) -> cb.lessThanOrEqualTo(r.get("price"), maxPrice);
    }

    public static Specification<ProductInfo> minRatingSpec(BigDecimal minRating) {
        return minRating == null ? NONE : (r, q, cb) -> cb.greaterThanOrEqualTo(r.get("averageRating"), minRating);
    }

    public static Specification<ProductInfo> brandNamesSpec(List<String> brandNames) {
        return (brandNames == null || brandNames.isEmpty()) ? NONE : (r, q, cb) -> r.get("brandName").in(brandNames);
    }

    public static Specification<ProductInfo> sellerNamesSpec(List<String> sellerNames) {
        return (sellerNames == null || sellerNames.isEmpty()) ? NONE : (r, q, cb) -> r.get("sellerName").in(sellerNames);
    }

    public static Specification<ProductInfo> nameContainsSpec(String keyword) {
        if (keyword == null || keyword.isBlank()) return NONE;
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (r, q, cb) -> cb.like(cb.lower(r.get("name")), pattern);
    }
}
