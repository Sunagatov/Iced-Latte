package com.zufar.onlinestore.product.service;

import com.zufar.onlinestore.product.dto.ProductPaginationDto;

import java.util.UUID;

public interface ProductApi {

    ProductPaginationDto getProducts(Integer page,
                                     Integer size,
                                     String sortAttribute,
                                     String sortDirection);
}