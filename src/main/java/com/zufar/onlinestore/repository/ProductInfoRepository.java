package com.zufar.onlinestore.repository;

import com.zufar.onlinestore.model.ProductInfo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductInfoRepository extends MongoRepository<ProductInfo, Integer> {
}
