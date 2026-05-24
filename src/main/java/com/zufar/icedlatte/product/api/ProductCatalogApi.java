package com.zufar.icedlatte.product.api;

import java.util.List;
import java.util.UUID;

import com.zufar.icedlatte.product.api.dto.ProductSnapshot;

/**
 * Stable contract for product catalog operations exposed to other modules. Other modules should depend on this
 * interface, not on concrete ProductService.
 */
public interface ProductCatalogApi {

    List<ProductSnapshot> getProductsByIds(List<UUID> ids);

    List<String> getSellerNames();

    List<String> getBrandNames();

    boolean existsById(UUID productId);
}
