package com.zufar.onlinestore.product.api;

import com.zufar.onlinestore.product.dto.ProductPaginationDto;
import com.zufar.onlinestore.product.dto.ProductResponseDto;

import java.util.UUID;

public interface ProductApi {

    ProductPaginationDto getAllProducts(Integer page,
                                        Integer size,
                                        String sortAttribute,
                                        String sortDirection);

    ProductResponseDto getProduct(UUID id);
}