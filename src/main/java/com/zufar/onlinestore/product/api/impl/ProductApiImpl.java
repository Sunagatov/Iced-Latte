package com.zufar.onlinestore.product.api.impl;

import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.product.dto.ProductPaginationDto;
import com.zufar.onlinestore.product.dto.ProductResponseDto;
import com.zufar.onlinestore.product.service.GetAllProducts;
import com.zufar.onlinestore.product.service.GetProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductApiImpl implements ProductApi {

    private final GetAllProducts getAllProducts;
    private final GetProduct getProduct;

    @Override
    public ProductPaginationDto getAllProducts(Integer page,
                                               Integer size,
                                               String sortAttribute,
                                               String sortDirection) {
        return getAllProducts.getProducts(page, size, sortAttribute, sortDirection);
    }

    @Override
    public ProductResponseDto getProduct(UUID id) {
        return getProduct.getProduct(id);
    }
}