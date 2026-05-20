package com.zufar.icedlatte.product.api;

import com.zufar.icedlatte.product.entity.ProductInfo;

import java.util.List;
import java.util.UUID;

/**
 * Provides JPA entity access for modules that hold @ManyToOne relationships to ProductInfo
 * (cart, favorite). Use {@link ProductCatalogApi} for DTO-level access instead.
 */
public interface ProductEntityProvider {

    List<ProductInfo> findAllById(Iterable<UUID> ids);
}
