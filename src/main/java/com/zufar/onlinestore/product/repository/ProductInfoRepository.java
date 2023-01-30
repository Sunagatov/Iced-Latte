package com.zufar.onlinestore.product.repository;

import com.zufar.onlinestore.product.entity.ProductInfo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInfoRepository extends MongoRepository<ProductInfo, Integer> {
}
