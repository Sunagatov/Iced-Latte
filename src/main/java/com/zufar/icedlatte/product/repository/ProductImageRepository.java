package com.zufar.icedlatte.product.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zufar.icedlatte.product.entity.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByPosition(UUID productId);

    List<ProductImage> findByProductIdInOrderByPosition(List<UUID> productIds);
}
