package com.zufar.onlinestore.product.repository;

import com.zufar.onlinestore.product.entity.ProductInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInfoRepository extends JpaRepository<ProductInfo, UUID> {

    @Query(value = "SELECT * FROM product WHERE id in :ids FOR UPDATE", nativeQuery = true)
    List<ProductInfo> findAllByIdForUpdate(@Param("ids") Iterable<UUID> ids);
}