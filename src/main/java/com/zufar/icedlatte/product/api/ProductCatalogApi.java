package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.api.dto.ProductPageSnapshot;
import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Stable contract for product catalog operations exposed to other modules.
 * Other modules should depend on this interface, not on concrete ProductService.
 */
public interface ProductCatalogApi {

    List<ProductSnapshot> getProductsByIds(List<UUID> ids);

    ProductPageSnapshot getProducts(
            Integer pageNumber, Integer pageSize,
            String sortAttribute, String sortDirection,
            BigDecimal minPrice, BigDecimal maxPrice,
            Integer minimumAverageRating,
            List<String> brandNames, List<String> sellerNames,
            String keyword);

    List<String> getSellerNames();

    List<String> getBrandNames();

    boolean existsById(UUID productId);
}
