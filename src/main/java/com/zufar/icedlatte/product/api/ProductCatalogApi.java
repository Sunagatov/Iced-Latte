package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.openapi.dto.ProductInfoDto;
import com.zufar.icedlatte.openapi.dto.ProductListWithPaginationInfoDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Stable contract for product catalog operations exposed to other modules.
 * Other modules should depend on this interface, not on concrete ProductService.
 */
public interface ProductCatalogApi {

    ProductInfoDto getProductById(UUID productId);

    List<ProductInfoDto> getProductsByIds(List<UUID> ids);

    ProductListWithPaginationInfoDto getProducts(
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
