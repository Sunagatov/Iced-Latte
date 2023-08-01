package com.zufar.onlinestore.product.api.impl;

import com.zufar.onlinestore.product.api.ProductApi;
import com.zufar.onlinestore.product.dto.ProductPaginationDto;
import com.zufar.onlinestore.product.service.GetAllProducts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProductApiImpl implements ProductApi {

    private final GetAllProducts getProducts;

    @Override
    public ProductPaginationDto getProducts(Integer page,
                                            Integer size,
                                            String sortAttribute,
                                            String sortDirection) {
        return getProducts.getProducts(page, size, sortAttribute, sortDirection);
    }
}