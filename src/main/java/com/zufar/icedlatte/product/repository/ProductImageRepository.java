package com.zufar.icedlatte.product.repository;

import com.zufar.icedlatte.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderByPosition(UUID productId);

    List<ProductImage> findByProductIdInOrderByPosition(List<UUID> productIds);
}
