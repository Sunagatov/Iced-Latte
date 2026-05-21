package com.zufar.icedlatte.product.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductSnapshot(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        Boolean active,
        String productFileUrl,
        List<String> productImageUrls,
        BigDecimal averageRating,
        Integer reviewsCount,
        String aiSummary,
        String brandName,
        String sellerName,
        String originCountry,
        Integer weight,
        Integer length,
        Integer width,
        Integer height,
        Integer soldProductsCount,
        Integer discount,
        OffsetDateTime dateAdded,
        Integer popularityScore
) {
}
