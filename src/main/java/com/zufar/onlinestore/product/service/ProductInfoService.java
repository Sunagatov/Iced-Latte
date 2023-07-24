package com.zufar.onlinestore.product.service;

import com.zufar.onlinestore.product.dto.ProductInfoRequestResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductInfoService {

    Page<ProductInfoRequestResponseDto> getAllProducts(Pageable pageable);
}