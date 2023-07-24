package com.zufar.onlinestore.product.service;

import com.zufar.onlinestore.product.dto.ProductInfoRequestResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.Collection;

public interface ProductInfoService {

    Collection<ProductInfoRequestResponseDto> getAllProducts(Pageable pageable);
}