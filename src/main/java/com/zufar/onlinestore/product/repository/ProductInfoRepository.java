package com.zufar.onlinestore.product.repository;

import com.zufar.onlinestore.product.entity.ProductInfo;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInfoRepository extends JpaRepository<ProductInfo, UUID> {
}